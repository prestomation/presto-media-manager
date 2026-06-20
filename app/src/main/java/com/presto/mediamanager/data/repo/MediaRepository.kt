package com.presto.mediamanager.data.repo

import com.presto.mediamanager.data.db.MediaDao
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.data.model.MediaStatus
import com.presto.mediamanager.data.settings.SettingsProvider
import com.presto.mediamanager.data.storage.StorageGateway
import com.presto.mediamanager.media.ExportOutcome
import com.presto.mediamanager.media.ExportRequest
import com.presto.mediamanager.media.VideoExporter
import com.presto.mediamanager.util.Filenames
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the review workflow: reconciles the input folder
 * with the Room mirror and performs the delete / later / archive / share actions.
 *
 * Depends only on small ports ([StorageGateway], [SettingsProvider],
 * [VideoExporter]) plus the DAO, so its logic is exercised by JVM tests.
 */
class MediaRepository(
    private val dao: MediaDao,
    private val storage: StorageGateway,
    private val settings: SettingsProvider,
    private val exporter: VideoExporter,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeReviewQueue(): Flow<List<MediaItem>> = dao.observeReviewQueue()
    fun observeReviewCount(): Flow<Int> = dao.observeReviewCount()
    suspend fun reviewCount(): Int = dao.reviewCount()

    suspend fun findForEditing(uri: String): MediaItem? = dao.findByUri(uri)

    /** Reconcile the input folder with the DB: add new videos, drop vanished ones. */
    suspend fun scanInputFolder(): Int {
        val inputUri = settings.current().inputFolderUri ?: return 0
        val videos = storage.listVideos(inputUri)
        val seenUris = videos.map { it.uri }.toSet()

        var added = 0
        for (video in videos) {
            if (dao.findByUri(video.uri) == null) {
                dao.insertIfNew(
                    MediaItem(
                        uri = video.uri,
                        displayName = video.displayName,
                        sizeBytes = video.sizeBytes,
                        durationMs = storage.probeDurationMs(video.uri),
                        dateCapturedMs = video.lastModifiedMs,
                        dateFirstSeenMs = now(),
                        status = MediaStatus.PENDING,
                    ),
                )
                added++
            }
        }

        // Drop rows whose file disappeared (deleted outside the app) and are still in-queue.
        for (uri in dao.allUris()) {
            if (uri !in seenUris) dao.deleteRow(uri)
        }
        return added
    }

    suspend fun delete(item: MediaItem) {
        storage.delete(item.uri)
        dao.deleteRow(item.uri)
    }

    suspend fun markLater(item: MediaItem) {
        dao.updateStatus(item.uri, MediaStatus.LATER, reviewedAtMs = null)
    }

    /** Quick archive: copy the original bytes (no re-encode) to the archive folder. */
    suspend fun quickArchive(item: MediaItem, label: String) {
        val archiveUri = settings.current().archiveFolderUri ?: return
        val fileName = Filenames.dated(label, item.dateCapturedMs)
        val written = storage.copyInto(item.uri, archiveUri, fileName)
        if (written != null) {
            storage.delete(item.uri)
            dao.deleteRow(item.uri)
        }
    }

    /** Edited export from the cropping screen. Always archives; SHARE also downscales. */
    suspend fun exportEdited(item: MediaItem, request: ExportRequest): ExportOutcome {
        val s = settings.current()
        val archiveFolder = requireNotNull(s.archiveFolderUri) { "Archive folder not set" }
        val shareFolder = requireNotNull(s.shareFolderUri) { "Share folder not set" }
        val outcome = exporter.export(
            sourceUri = item.uri,
            captureMs = item.dateCapturedMs,
            archiveFolderUri = archiveFolder,
            shareFolderUri = shareFolder,
            request = request,
        )
        storage.delete(item.uri)
        dao.deleteRow(item.uri)
        return outcome
    }

    /** Auto-delete unreviewed items older than [days]. Returns count removed. */
    suspend fun autoDeleteOlderThan(days: Int): Int {
        val cutoff = now() - days.toLong() * 24L * 60L * 60L * 1000L
        val stale = dao.staleItems(cutoff)
        for (item in stale) {
            storage.delete(item.uri)
            dao.deleteRow(item.uri)
        }
        return stale.size
    }
}

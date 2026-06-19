package com.presto.mediamanager.data.repo

import android.net.Uri
import com.presto.mediamanager.data.db.MediaDao
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.data.model.MediaStatus
import com.presto.mediamanager.data.saf.SafManager
import com.presto.mediamanager.data.settings.SettingsRepository
import com.presto.mediamanager.media.ExportManager
import com.presto.mediamanager.media.ExportOutcome
import com.presto.mediamanager.media.ExportRequest
import com.presto.mediamanager.util.Filenames
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the review workflow: reconciles the input folder
 * with the Room mirror and performs the delete / later / archive / share actions.
 */
class MediaRepository(
    private val dao: MediaDao,
    private val saf: SafManager,
    private val settings: SettingsRepository,
    private val exporter: ExportManager,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeReviewQueue(): Flow<List<MediaItem>> = dao.observeReviewQueue()
    fun observeReviewCount(): Flow<Int> = dao.observeReviewCount()
    suspend fun reviewCount(): Int = dao.reviewCount()

    suspend fun findForEditing(uri: String): MediaItem? = dao.findByUri(uri)

    /** Reconcile the input folder with the DB: add new videos, drop vanished ones. */
    suspend fun scanInputFolder(): Int {
        val inputUri = settings.current().inputFolderUri ?: return 0
        val videos = saf.listVideos(Uri.parse(inputUri))
        val seenUris = videos.map { it.uri }.toSet()

        var added = 0
        for (video in videos) {
            if (dao.findByUri(video.uri) == null) {
                dao.insertIfNew(
                    MediaItem(
                        uri = video.uri,
                        displayName = video.displayName,
                        sizeBytes = video.sizeBytes,
                        durationMs = saf.probeDurationMs(Uri.parse(video.uri)),
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
        saf.delete(Uri.parse(item.uri))
        dao.deleteRow(item.uri)
    }

    suspend fun markLater(item: MediaItem) {
        dao.updateStatus(item.uri, MediaStatus.LATER, reviewedAtMs = null)
    }

    /** Quick archive: copy the original bytes (no re-encode) to the archive folder. */
    suspend fun quickArchive(item: MediaItem, label: String) {
        val archiveUri = settings.current().archiveFolderUri ?: return
        val fileName = Filenames.dated(label, item.dateCapturedMs)
        val written = saf.copyInto(Uri.parse(item.uri), Uri.parse(archiveUri), fileName)
        if (written != null) {
            saf.delete(Uri.parse(item.uri))
            dao.deleteRow(item.uri)
        }
    }

    /** Edited export from the cropping screen. Always archives; SHARE also downscales. */
    suspend fun exportEdited(item: MediaItem, request: ExportRequest): ExportOutcome {
        val s = settings.current()
        val archiveTree = requireNotNull(s.archiveFolderUri) { "Archive folder not set" }
        val shareTree = requireNotNull(s.shareFolderUri) { "Share folder not set" }
        val outcome = exporter.export(
            sourceUri = Uri.parse(item.uri),
            captureMs = item.dateCapturedMs,
            archiveTreeUri = Uri.parse(archiveTree),
            shareTreeUri = Uri.parse(shareTree),
            request = request,
        )
        saf.delete(Uri.parse(item.uri))
        dao.deleteRow(item.uri)
        return outcome
    }

    /** Auto-delete unreviewed items older than [days]. Returns count removed. */
    suspend fun autoDeleteOlderThan(days: Int): Int {
        val cutoff = now() - days.toLong() * 24L * 60L * 60L * 1000L
        val stale = dao.staleItems(cutoff)
        for (item in stale) {
            saf.delete(Uri.parse(item.uri))
            dao.deleteRow(item.uri)
        }
        return stale.size
    }
}

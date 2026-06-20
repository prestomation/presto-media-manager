package com.presto.mediamanager.fakes

import com.presto.mediamanager.data.settings.AppSettings
import com.presto.mediamanager.data.settings.SettingsProvider
import com.presto.mediamanager.data.storage.StorageGateway
import com.presto.mediamanager.data.storage.StoredVideo
import com.presto.mediamanager.media.ExportOutcome
import com.presto.mediamanager.media.ExportRequest
import com.presto.mediamanager.media.VideoExporter
import java.io.File

/** [StorageGateway] backed by real temp directories; folder/file URIs are file paths. */
class FakeStorageGateway(private val probeMs: Long = 1_000L) : StorageGateway {
    override fun persistFolderPermission(folderUri: String) = Unit

    override suspend fun listVideos(folderUri: String): List<StoredVideo> =
        File(folderUri).listFiles { f -> f.isFile && f.extension == "mp4" }
            ?.sortedBy { it.name }
            ?.map { StoredVideo(it.absolutePath, it.name, it.length(), it.lastModified()) }
            ?: emptyList()

    override suspend fun delete(uri: String): Boolean = File(uri).delete()

    override suspend fun copyInto(sourceUri: String, destFolderUri: String, fileName: String): String? {
        val dest = File(destFolderUri, fileName)
        File(sourceUri).copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    override suspend fun writeFileInto(file: File, destFolderUri: String, fileName: String): String? {
        val dest = File(destFolderUri, fileName)
        file.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    override suspend fun probeDurationMs(uri: String): Long = probeMs
}

class FakeSettingsProvider(var settings: AppSettings) : SettingsProvider {
    override suspend fun current(): AppSettings = settings
}

/** No-op exporter; the export path needs real codecs and is covered by the emulator suite. */
class FakeVideoExporter : VideoExporter {
    override suspend fun export(
        sourceUri: String,
        captureMs: Long,
        archiveFolderUri: String,
        shareFolderUri: String,
        request: ExportRequest,
    ): ExportOutcome = ExportOutcome(archiveUri = "$archiveFolderUri/out.mp4", shareUri = null)
}

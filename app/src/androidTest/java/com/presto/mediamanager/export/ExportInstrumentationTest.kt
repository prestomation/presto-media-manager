package com.presto.mediamanager.export

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.presto.mediamanager.data.settings.ShareResolution
import com.presto.mediamanager.data.storage.StorageGateway
import com.presto.mediamanager.data.storage.StoredVideo
import com.presto.mediamanager.media.CropRect
import com.presto.mediamanager.media.ExportDestination
import com.presto.mediamanager.media.ExportManager
import com.presto.mediamanager.media.ExportRequest
import com.presto.mediamanager.media.TrimRange
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real Media3 Transformer export on the device/emulator: generate a 720p clip,
 * export with a trim + downscale + audio-strip, and assert the archive copy is
 * full-res while the share copy is downscaled — the core feature that JVM tests
 * can't cover because it needs real codecs.
 */
@RunWith(AndroidJUnit4::class)
class ExportInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Minimal directory-backed gateway: folder URIs are directory paths. */
    private class DirStorageGateway : StorageGateway {
        override fun persistFolderPermission(folderUri: String) = Unit
        override suspend fun listVideos(folderUri: String): List<StoredVideo> = emptyList()
        override suspend fun delete(uri: String): Boolean = File(uri).delete()
        override suspend fun copyInto(sourceUri: String, destFolderUri: String, fileName: String): String? {
            val dest = File(destFolderUri, fileName)
            File(sourceUri).copyTo(dest, overwrite = true)
            return dest.absolutePath
        }
        override suspend fun writeFileInto(file: File, destFolderUri: String, fileName: String): String {
            val dest = File(destFolderUri, fileName)
            file.copyTo(dest, overwrite = true)
            return dest.absolutePath
        }
        override suspend fun probeDurationMs(uri: String): Long = 0L
    }

    @Test
    fun export_trimsDownscalesAndWritesBothOutputs() = runBlocking {
        val input = File(context.cacheDir, "export-input.mp4")
        TestVideoFactory.createVideoOnlyMp4(input, width = 1280, height = 720, frameRate = 24, frameCount = 36)
        assertThat(input.length()).isGreaterThan(0L)

        val archiveDir = File(context.cacheDir, "archive").apply { mkdirs() }
        val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
        val exporter = ExportManager(context, DirStorageGateway())

        val request = ExportRequest(
            label = "near-miss",
            trim = TrimRange(0, 800),
            crop = CropRect(),
            removeAudio = true,
            shareResolution = ShareResolution.P480,
            destination = ExportDestination.SHARE,
        )

        val outcome = exporter.export(
            sourceUri = Uri.fromFile(input).toString(),
            captureMs = 1_718_000_000_000L,
            archiveFolderUri = archiveDir.absolutePath,
            shareFolderUri = shareDir.absolutePath,
            request = request,
        )

        // Archive copy: full resolution, dated/labeled name.
        val archiveFile = File(outcome.archiveUri)
        assertThat(archiveFile.exists()).isTrue()
        assertThat(archiveFile.name).matches("\\d{4}-\\d{2}-\\d{2}_near-miss\\.mp4")
        assertThat(videoHeight(archiveFile)).isEqualTo(720)
        assertThat(hasAudioTrack(archiveFile)).isFalse() // audio removed

        // Share copy: downscaled to 480p.
        val shareFile = File(outcome.shareUri!!)
        assertThat(shareFile.exists()).isTrue()
        assertThat(videoHeight(shareFile)).isEqualTo(480)

        // Trimmed to ~800ms (allow generous tolerance for keyframe alignment).
        assertThat(durationMs(archiveFile)).isLessThan(1_500L)
    }

    private fun videoHeight(file: File): Int = withVideoFormat(file) { it.getInteger(MediaFormat.KEY_HEIGHT) }

    private fun hasAudioTrack(file: File): Boolean {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) return true
            }
            return false
        } finally {
            extractor.release()
        }
    }

    private fun <T> withVideoFormat(file: File, block: (MediaFormat) -> T): T {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    return block(format)
                }
            }
            error("No video track in ${file.name}")
        } finally {
            extractor.release()
        }
    }

    private fun durationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } finally {
            retriever.release()
        }
    }
}

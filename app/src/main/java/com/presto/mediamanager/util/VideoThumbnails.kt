package com.presto.mediamanager.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Extracts evenly-spaced frames for the trim-bar filmstrip. */
object VideoThumbnails {
    private const val COUNT = 8

    // Thumbnail extraction is best-effort: any decoder/IO failure just yields no
    // filmstrip rather than breaking the editor.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun extract(
        context: Context,
        uri: String,
        durationMs: Long,
        count: Int = COUNT,
    ): List<ImageBitmap> = withContext(Dispatchers.IO) {
        if (durationMs <= 0) return@withContext emptyList()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))
            (0 until count).mapNotNull { i ->
                val timeUs = durationMs * 1000L * i / count
                retriever.getScaledFrameAt(timeUs)?.asImageBitmap()
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            retriever.release()
        }
    }

    private fun MediaMetadataRetriever.getScaledFrameAt(timeUs: Long): Bitmap? =
        getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
}

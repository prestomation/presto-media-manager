package com.presto.mediamanager.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.presto.mediamanager.data.saf.SafManager
import com.presto.mediamanager.util.Filenames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Outcome of an export: the URIs written into the archive/share folders. */
data class ExportOutcome(
    val archiveUri: Uri,
    val shareUri: Uri?,
)

/**
 * Renders edited clips with Media3 Transformer. Always writes a full-resolution
 * edited file to the archive folder; for SHARE it also writes a downscaled copy
 * to the share folder. Both files share the same "{date}_{label}.mp4" stem.
 */
class ExportManager(
    private val context: Context,
    private val saf: SafManager,
) {
    suspend fun export(
        sourceUri: Uri,
        captureMs: Long,
        archiveTreeUri: Uri,
        shareTreeUri: Uri,
        request: ExportRequest,
    ): ExportOutcome {
        val fileName = Filenames.dated(request.label, captureMs)

        // 1. Full-resolution edited copy -> archive.
        val fullRes = File.createTempFile("export-full", ".mp4", context.cacheDir)
        runTransform(sourceUri, request, presentationHeight = null, outFile = fullRes)
        val archiveUri = saf.writeFileInto(fullRes, archiveTreeUri, fileName)
            ?: error("Failed to write archive file")
        fullRes.delete()

        // 2. For SHARE, a downscaled copy -> share folder.
        var shareUri: Uri? = null
        if (request.destination == ExportDestination.SHARE) {
            val shareFile = File.createTempFile("export-share", ".mp4", context.cacheDir)
            runTransform(
                sourceUri,
                request,
                presentationHeight = request.shareResolution.height,
                outFile = shareFile,
            )
            shareUri = saf.writeFileInto(shareFile, shareTreeUri, fileName)
                ?: error("Failed to write share file")
            shareFile.delete()
        }

        return ExportOutcome(archiveUri, shareUri)
    }

    private suspend fun runTransform(
        sourceUri: Uri,
        request: ExportRequest,
        presentationHeight: Int?,
        outFile: File,
    ) = withContext(Dispatchers.Main) {
        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(request.trim.startMs)
                    .setEndPositionMs(request.trim.endMs)
                    .build(),
            )
            .build()

        val videoEffects = buildList<Effect> {
            if (!request.crop.isFullFrame) add(request.crop.toMedia3Crop())
            if (presentationHeight != null) {
                add(Presentation.createForHeight(presentationHeight))
            }
        }

        val edited = EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(request.removeAudio)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException,
                    ) {
                        if (cont.isActive) cont.resumeWithException(exception)
                    }
                })
                .build()

            cont.invokeOnCancellation { transformer.cancel() }
            transformer.start(edited, outFile.absolutePath)
        }
    }
}

/**
 * Convert a top-left normalized [0,1] rect to Media3's NDC crop, where the frame
 * spans [-1,1] on both axes and y increases upward.
 */
private fun CropRect.toMedia3Crop(): Crop {
    val ndcLeft = left * 2f - 1f
    val ndcRight = right * 2f - 1f
    val ndcTop = 1f - top * 2f
    val ndcBottom = 1f - bottom * 2f
    return Crop(ndcLeft, ndcRight, ndcBottom, ndcTop)
}

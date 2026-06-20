package com.presto.mediamanager.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.presto.mediamanager.media.TrimRange

private const val MIN_CLIP_MS = 250L

/**
 * Scrub bar with a filmstrip background, draggable start/end trim handles, and a
 * live playhead. The shaded region between the handles is the kept clip;
 * everything outside it is dimmed.
 */
@Composable
fun TrimBar(
    durationMs: Long,
    trim: TrimRange,
    positionMs: Long,
    thumbnails: List<ImageBitmap>,
    onTrimChange: (TrimRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0) return
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1F26)),
    ) {
        val density = LocalDensity.current
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val startFrac = (trim.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
        val endFrac = (trim.endMs.toFloat() / durationMs).coerceIn(0f, 1f)

        // Filmstrip background.
        if (thumbnails.isNotEmpty()) {
            Row(Modifier.fillMaxSize()) {
                thumbnails.forEach { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }

        // Dim the trimmed-out regions.
        val dim = Color.Black.copy(alpha = 0.55f)
        Box(Modifier.offset(x = 0.dp).width(maxWidth * startFrac).fillMaxHeight().background(dim))
        Box(
            Modifier.offset(x = maxWidth * endFrac)
                .width(maxWidth * (1f - endFrac)).fillMaxHeight().background(dim),
        )

        // Selected region outline.
        Box(
            Modifier
                .offset(x = maxWidth * startFrac)
                .width(maxWidth * (endFrac - startFrac))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        )

        // Playhead (dark outline + white core so it reads over any frame).
        val posFrac = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        Box(
            Modifier
                .offset(x = maxWidth * posFrac - 2.dp)
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.6f)),
        )
        Box(
            Modifier
                .offset(x = maxWidth * posFrac - 1.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White),
        )

        Handle(startFrac, maxWidth) { deltaPx ->
            val deltaMs = (deltaPx / trackWidthPx * durationMs).toLong()
            onTrimChange(trim.copy(startMs = (trim.startMs + deltaMs).coerceIn(0, trim.endMs - MIN_CLIP_MS)))
        }
        Handle(endFrac, maxWidth) { deltaPx ->
            val deltaMs = (deltaPx / trackWidthPx * durationMs).toLong()
            onTrimChange(trim.copy(endMs = (trim.endMs + deltaMs).coerceIn(trim.startMs + MIN_CLIP_MS, durationMs)))
        }
    }
}

@Composable
private fun Handle(offsetFrac: Float, trackWidth: Dp, onDrag: (Float) -> Unit) {
    Box(
        Modifier
            .offset(x = trackWidth * offsetFrac - 8.dp)
            .width(16.dp)
            .fillMaxHeight()
            .background(Color.White, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, delta -> onDrag(delta) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(2.dp).height(20.dp).background(Color(0xFF1A1F26), RoundedCornerShape(1.dp)))
    }
}

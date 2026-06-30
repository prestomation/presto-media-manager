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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
    onScrub: (Long?) -> Unit = {},
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

        // Snapshot the trim at gesture start; apply the cumulative drag so the
        // handle tracks the finger regardless of recomposition timing.
        var startTrim by remember { mutableStateOf(trim) }
        val latestTrim by rememberUpdatedState(trim)
        val onChange by rememberUpdatedState(onTrimChange)
        val onScrubCb by rememberUpdatedState(onScrub)

        // Each handle reports the frame it's parked on (onScrub) so the preview can
        // hold that exact frame live; null on release resumes normal playback.
        Handle(
            startFrac, maxWidth,
            onDragStart = { startTrim = latestTrim; onScrubCb(latestTrim.startMs) },
            onDragEnd = { onScrubCb(null) },
        ) { totalPx ->
            val deltaMs = (totalPx / trackWidthPx * durationMs).toLong()
            val s = startTrim
            val newStart = (s.startMs + deltaMs).coerceIn(0, s.endMs - MIN_CLIP_MS)
            onChange(s.copy(startMs = newStart))
            onScrubCb(newStart)
        }
        Handle(
            endFrac, maxWidth,
            onDragStart = { startTrim = latestTrim; onScrubCb(latestTrim.endMs) },
            onDragEnd = { onScrubCb(null) },
        ) { totalPx ->
            val deltaMs = (totalPx / trackWidthPx * durationMs).toLong()
            val s = startTrim
            val newEnd = (s.endMs + deltaMs).coerceIn(s.startMs + MIN_CLIP_MS, durationMs)
            onChange(s.copy(endMs = newEnd))
            onScrubCb(newEnd)
        }
    }
}

@Composable
private fun Handle(
    offsetFrac: Float,
    trackWidth: Dp,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onCumulativeDrag: (Float) -> Unit,
) {
    val startCb by rememberUpdatedState(onDragStart)
    val endCb by rememberUpdatedState(onDragEnd)
    val dragCb by rememberUpdatedState(onCumulativeDrag)
    Box(
        Modifier
            .offset(x = trackWidth * offsetFrac - 8.dp)
            .width(16.dp)
            .fillMaxHeight()
            .background(Color.White, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f; startCb() },
                    onDragEnd = { endCb() },
                    onDragCancel = { endCb() },
                    onHorizontalDrag = { _, delta -> acc += delta; dragCb(acc) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(2.dp).height(20.dp).background(Color(0xFF1A1F26), RoundedCornerShape(1.dp)))
    }
}

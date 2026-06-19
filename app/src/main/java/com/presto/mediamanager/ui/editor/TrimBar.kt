package com.presto.mediamanager.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.presto.mediamanager.media.TrimRange

/**
 * A scrub bar with draggable start/end handles selecting a [TrimRange] over
 * [durationMs]. The shaded region between the handles is the kept clip.
 */
@Composable
fun TrimBar(
    durationMs: Long,
    trim: TrimRange,
    onTrimChange: (TrimRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0) return
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF1A1F26), RoundedCornerShape(8.dp)),
    ) {
        val density = LocalDensity.current
        val trackWidthPx = with(density) { maxWidth.toPx() }
        val startFrac = trim.startMs.toFloat() / durationMs
        val endFrac = trim.endMs.toFloat() / durationMs

        // Selected region
        Box(
            Modifier
                .offset(x = maxWidth * startFrac)
                .width(maxWidth * (endFrac - startFrac))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        )

        Handle(
            offsetFrac = startFrac,
            trackWidth = maxWidth,
            alignment = Alignment.CenterStart,
        ) { deltaPx ->
            val deltaMs = (deltaPx / trackWidthPx * durationMs).toLong()
            val newStart = (trim.startMs + deltaMs).coerceIn(0, trim.endMs - 250)
            onTrimChange(trim.copy(startMs = newStart))
        }
        Handle(
            offsetFrac = endFrac,
            trackWidth = maxWidth,
            alignment = Alignment.CenterStart,
        ) { deltaPx ->
            val deltaMs = (deltaPx / trackWidthPx * durationMs).toLong()
            val newEnd = (trim.endMs + deltaMs).coerceIn(trim.startMs + 250, durationMs)
            onTrimChange(trim.copy(endMs = newEnd))
        }
    }
}

@Composable
private fun Handle(
    offsetFrac: Float,
    trackWidth: androidx.compose.ui.unit.Dp,
    alignment: Alignment,
    onDrag: (Float) -> Unit,
) {
    Box(
        Modifier
            .offset(x = trackWidth * offsetFrac - 8.dp)
            .width(16.dp)
            .fillMaxHeight()
            .background(Color.White, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, delta -> onDrag(delta) }
            },
    )
}

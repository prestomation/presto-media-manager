package com.presto.mediamanager.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.presto.mediamanager.media.CropRect

private const val MIN_SIZE = 0.12f
private val HANDLE = 22.dp

/**
 * Draggable crop rectangle over the video with 4 corner and 4 edge handles, a
 * dimmed scrim over the cropped-out area, and optional aspect-ratio lock. Holds
 * a normalized [CropRect] (0..1, top-left origin).
 *
 * Each gesture snapshots the rect at drag-start and applies the *cumulative*
 * delta, so dragging tracks the finger regardless of recomposition timing.
 */
@Composable
fun CropOverlay(
    crop: CropRect,
    onCropChange: (CropRect) -> Unit,
    aspectLocked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val latestCrop by rememberUpdatedState(crop)
        val onChange by rememberUpdatedState(onCropChange)
        val locked by rememberUpdatedState(aspectLocked)

        // Snapshot of the rect at the start of the active gesture.
        var startCrop by remember { mutableStateOf(crop) }

        CropScrim(crop)

        // Body: drag to move the whole rect.
        Box(
            Modifier
                .offset(x = maxWidth * crop.left, y = maxHeight * crop.top)
                .size(maxWidth * (crop.right - crop.left), maxHeight * (crop.bottom - crop.top))
                .border(2.dp, MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    var acc = Offset.Zero
                    detectDragGestures(
                        onDragStart = { acc = Offset.Zero; startCrop = latestCrop },
                        onDrag = { _, drag ->
                            acc += drag
                            val s = startCrop
                            val w = s.right - s.left
                            val h = s.bottom - s.top
                            val nl = (s.left + acc.x / wPx).coerceIn(0f, 1f - w)
                            val nt = (s.top + acc.y / hPx).coerceIn(0f, 1f - h)
                            onChange(s.copy(left = nl, top = nt, right = nl + w, bottom = nt + h))
                        },
                    )
                },
        )

        // Corner handles.
        Handle(maxWidth * crop.left, maxHeight * crop.top, { startCrop = latestCrop }) { dx, dy ->
            onChange(resizeCorner(startCrop, dx / wPx, dy / hPx, left = true, top = true, locked))
        }
        Handle(maxWidth * crop.right, maxHeight * crop.top, { startCrop = latestCrop }) { dx, dy ->
            onChange(resizeCorner(startCrop, dx / wPx, dy / hPx, left = false, top = true, locked))
        }
        Handle(maxWidth * crop.left, maxHeight * crop.bottom, { startCrop = latestCrop }) { dx, dy ->
            onChange(resizeCorner(startCrop, dx / wPx, dy / hPx, left = true, top = false, locked))
        }
        Handle(maxWidth * crop.right, maxHeight * crop.bottom, { startCrop = latestCrop }) { dx, dy ->
            onChange(resizeCorner(startCrop, dx / wPx, dy / hPx, left = false, top = false, locked))
        }

        // Edge handles (hidden while aspect is locked, since they'd break the ratio).
        if (!aspectLocked) {
            val midX = maxWidth * (crop.left + crop.right) / 2
            val midY = maxHeight * (crop.top + crop.bottom) / 2
            Handle(midX, maxHeight * crop.top, { startCrop = latestCrop }) { _, dy ->
                val s = startCrop
                onChange(s.copy(top = (s.top + dy / hPx).coerceIn(0f, s.bottom - MIN_SIZE)))
            }
            Handle(midX, maxHeight * crop.bottom, { startCrop = latestCrop }) { _, dy ->
                val s = startCrop
                onChange(s.copy(bottom = (s.bottom + dy / hPx).coerceIn(s.top + MIN_SIZE, 1f)))
            }
            Handle(maxWidth * crop.left, midY, { startCrop = latestCrop }) { dx, _ ->
                val s = startCrop
                onChange(s.copy(left = (s.left + dx / wPx).coerceIn(0f, s.right - MIN_SIZE)))
            }
            Handle(maxWidth * crop.right, midY, { startCrop = latestCrop }) { dx, _ ->
                val s = startCrop
                onChange(s.copy(right = (s.right + dx / wPx).coerceIn(s.left + MIN_SIZE, 1f)))
            }
        }
    }
}

/** Four dim rectangles covering everything outside the crop. */
@Composable
private fun BoxWithConstraintsScope.CropScrim(crop: CropRect) {
    val scrim = Color.Black.copy(alpha = 0.5f)
    Box(Modifier.size(maxWidth, maxHeight * crop.top).background(scrim))
    Box(
        Modifier.offset(y = maxHeight * crop.bottom)
            .size(maxWidth, maxHeight * (1f - crop.bottom)).background(scrim),
    )
    Box(
        Modifier.offset(y = maxHeight * crop.top)
            .size(maxWidth * crop.left, maxHeight * (crop.bottom - crop.top)).background(scrim),
    )
    Box(
        Modifier.offset(x = maxWidth * crop.right, y = maxHeight * crop.top)
            .size(maxWidth * (1f - crop.right), maxHeight * (crop.bottom - crop.top)).background(scrim),
    )
}

/** A round handle that reports the cumulative drag (in px) since the gesture started. */
@Composable
private fun BoxWithConstraintsScope.Handle(
    centerX: Dp,
    centerY: Dp,
    onDragStart: () -> Unit,
    onCumulativeDrag: (Float, Float) -> Unit,
) {
    val half = HANDLE / 2
    val startCb by rememberUpdatedState(onDragStart)
    val dragCb by rememberUpdatedState(onCumulativeDrag)
    Box(
        Modifier
            .offset(x = centerX - half, y = centerY - half)
            .size(HANDLE)
            .background(Color.White, CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
            .pointerInput(Unit) {
                var acc = Offset.Zero
                detectDragGestures(
                    onDragStart = { acc = Offset.Zero; startCb() },
                    onDrag = { _, drag -> acc += drag; dragCb(acc.x, acc.y) },
                )
            },
    )
}

/** Resize from a corner using the start rect + cumulative delta; keeps aspect when [locked]. */
private fun resizeCorner(
    start: CropRect,
    dx: Float,
    dy: Float,
    left: Boolean,
    top: Boolean,
    locked: Boolean,
): CropRect {
    var l = start.left
    var t = start.top
    var r = start.right
    var b = start.bottom
    if (left) l = (l + dx).coerceIn(0f, r - MIN_SIZE) else r = (r + dx).coerceIn(l + MIN_SIZE, 1f)
    if (top) t = (t + dy).coerceIn(0f, b - MIN_SIZE) else b = (b + dy).coerceIn(t + MIN_SIZE, 1f)

    if (locked) {
        val aspect = (start.right - start.left).coerceAtLeast(0.001f) /
            (start.bottom - start.top).coerceAtLeast(0.001f)
        val newH = ((r - l) / aspect).coerceIn(MIN_SIZE, 1f)
        if (top) t = (b - newH).coerceIn(0f, b - MIN_SIZE) else b = (t + newH).coerceIn(t + MIN_SIZE, 1f)
    }
    return start.copy(left = l, top = t, right = r, bottom = b)
}

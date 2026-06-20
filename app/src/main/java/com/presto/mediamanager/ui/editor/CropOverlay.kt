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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        val aspect = remember(crop) {
            (crop.right - crop.left).coerceAtLeast(0.001f) /
                (crop.bottom - crop.top).coerceAtLeast(0.001f)
        }

        CropScrim(crop)

        // Body: drag to move the whole rect.
        Box(
            Modifier
                .offset(x = maxWidth * crop.left, y = maxHeight * crop.top)
                .size(maxWidth * (crop.right - crop.left), maxHeight * (crop.bottom - crop.top))
                .border(2.dp, MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val w = crop.right - crop.left
                        val h = crop.bottom - crop.top
                        val nl = (crop.left + drag.x / wPx).coerceIn(0f, 1f - w)
                        val nt = (crop.top + drag.y / hPx).coerceIn(0f, 1f - h)
                        onCropChange(crop.copy(left = nl, top = nt, right = nl + w, bottom = nt + h))
                    }
                },
        )

        // Corner handles.
        Handle(maxWidth * crop.left, maxHeight * crop.top) { dx, dy ->
            onCropChange(resizeCorner(crop, dx / wPx, dy / hPx, true, true, aspect, aspectLocked))
        }
        Handle(maxWidth * crop.right, maxHeight * crop.top) { dx, dy ->
            onCropChange(resizeCorner(crop, dx / wPx, dy / hPx, false, true, aspect, aspectLocked))
        }
        Handle(maxWidth * crop.left, maxHeight * crop.bottom) { dx, dy ->
            onCropChange(resizeCorner(crop, dx / wPx, dy / hPx, true, false, aspect, aspectLocked))
        }
        Handle(maxWidth * crop.right, maxHeight * crop.bottom) { dx, dy ->
            onCropChange(resizeCorner(crop, dx / wPx, dy / hPx, false, false, aspect, aspectLocked))
        }

        // Edge handles (hidden while aspect is locked, since they'd break the ratio).
        if (!aspectLocked) {
            val midX = maxWidth * (crop.left + crop.right) / 2
            val midY = maxHeight * (crop.top + crop.bottom) / 2
            Handle(midX, maxHeight * crop.top) { _, dy ->
                onCropChange(crop.copy(top = (crop.top + dy / hPx).coerceIn(0f, crop.bottom - MIN_SIZE)))
            }
            Handle(midX, maxHeight * crop.bottom) { _, dy ->
                onCropChange(crop.copy(bottom = (crop.bottom + dy / hPx).coerceIn(crop.top + MIN_SIZE, 1f)))
            }
            Handle(maxWidth * crop.left, midY) { dx, _ ->
                onCropChange(crop.copy(left = (crop.left + dx / wPx).coerceIn(0f, crop.right - MIN_SIZE)))
            }
            Handle(maxWidth * crop.right, midY) { dx, _ ->
                onCropChange(crop.copy(right = (crop.right + dx / wPx).coerceIn(crop.left + MIN_SIZE, 1f)))
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

@Composable
private fun BoxWithConstraintsScope.Handle(
    centerX: Dp,
    centerY: Dp,
    onDrag: (Float, Float) -> Unit,
) {
    val half = HANDLE / 2
    Box(
        Modifier
            .offset(x = centerX - half, y = centerY - half)
            .size(HANDLE)
            .background(Color.White, CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { _, drag -> onDrag(drag.x, drag.y) }
            },
    )
}

/** Resize from a corner; when [locked] keep [aspect] (w/h) by deriving height from width. */
private fun resizeCorner(
    crop: CropRect,
    dx: Float,
    dy: Float,
    left: Boolean,
    top: Boolean,
    aspect: Float,
    locked: Boolean,
): CropRect {
    var l = crop.left
    var t = crop.top
    var r = crop.right
    var b = crop.bottom
    if (left) l = (l + dx).coerceIn(0f, r - MIN_SIZE) else r = (r + dx).coerceIn(l + MIN_SIZE, 1f)
    if (top) t = (t + dy).coerceIn(0f, b - MIN_SIZE) else b = (b + dy).coerceIn(t + MIN_SIZE, 1f)

    if (locked) {
        val newH = ((r - l) / aspect).coerceIn(MIN_SIZE, 1f)
        if (top) t = (b - newH).coerceIn(0f, b - MIN_SIZE) else b = (t + newH).coerceIn(t + MIN_SIZE, 1f)
    }
    return crop.copy(left = l, top = t, right = r, bottom = b)
}

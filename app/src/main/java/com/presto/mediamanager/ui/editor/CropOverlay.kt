package com.presto.mediamanager.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.presto.mediamanager.media.CropRect
import androidx.compose.foundation.layout.Box

/**
 * Draggable/resizable crop rectangle over the video. Maintains a normalized
 * [CropRect] (0..1, top-left origin). Drag the body to move; drag a corner to resize.
 */
@Composable
fun CropOverlay(
    crop: CropRect,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }

        val leftDp = maxWidth * crop.left
        val topDp = maxHeight * crop.top
        val rectW = maxWidth * (crop.right - crop.left)
        val rectH = maxHeight * (crop.bottom - crop.top)

        Box(
            Modifier
                .offset(x = leftDp, y = topDp)
                .size(rectW, rectH)
                .border(2.dp, MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val dx = drag.x / wPx
                        val dy = drag.y / hPx
                        val width = crop.right - crop.left
                        val height = crop.bottom - crop.top
                        val newLeft = (crop.left + dx).coerceIn(0f, 1f - width)
                        val newTop = (crop.top + dy).coerceIn(0f, 1f - height)
                        onCropChange(
                            crop.copy(
                                left = newLeft,
                                top = newTop,
                                right = newLeft + width,
                                bottom = newTop + height,
                            ),
                        )
                    }
                },
        ) {
            CornerHandle(Modifier.align(androidx.compose.ui.Alignment.TopStart)) { dx, dy ->
                onCropChange(crop.copy(left = (crop.left + dx / wPx).coerceIn(0f, crop.right - 0.1f),
                    top = (crop.top + dy / hPx).coerceIn(0f, crop.bottom - 0.1f)))
            }
            CornerHandle(Modifier.align(androidx.compose.ui.Alignment.BottomEnd)) { dx, dy ->
                onCropChange(crop.copy(right = (crop.right + dx / wPx).coerceIn(crop.left + 0.1f, 1f),
                    bottom = (crop.bottom + dy / hPx).coerceIn(crop.top + 0.1f, 1f)))
            }
        }
    }
}

@Composable
private fun CornerHandle(modifier: Modifier, onDrag: (Float, Float) -> Unit) {
    Box(
        modifier
            .size(24.dp)
            .background(Color.White, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { _, drag -> onDrag(drag.x, drag.y) }
            },
    )
}

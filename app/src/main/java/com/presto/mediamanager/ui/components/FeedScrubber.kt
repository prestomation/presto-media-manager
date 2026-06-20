package com.presto.mediamanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.Locale

/**
 * Thin seek bar for the review feed: a slider plus current/total time. Absolute
 * seeking (the value is the position), so there's no relative-drag state to go
 * stale.
 */
@Composable
fun FeedScrubber(
    positionMs: Long,
    durationMs: Long,
    onScrubStart: () -> Unit,
    onSeek: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0) return
    Column(modifier.fillMaxWidth()) {
        Slider(
            value = positionMs.coerceIn(0, durationMs).toFloat(),
            valueRange = 0f..durationMs.toFloat(),
            onValueChange = {
                onScrubStart()
                onSeek(it.toLong())
            },
            onValueChangeFinished = onScrubEnd,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text(formatTime(durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

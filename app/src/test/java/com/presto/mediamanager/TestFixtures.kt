package com.presto.mediamanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.data.model.MediaStatus

/** Deterministic capture date so golden screenshots are stable. */
const val FIXED_CAPTURE_MS = 1_718_000_000_000L // 2024-06-10

fun sampleItem(
    id: String = "1",
    name: String = "VID_20240610_$id.mp4",
    durationMs: Long = 12_000,
): MediaItem = MediaItem(
    uri = "content://fake/$id",
    displayName = name,
    sizeBytes = 48_000_000,
    durationMs = durationMs,
    dateCapturedMs = FIXED_CAPTURE_MS,
    dateFirstSeenMs = FIXED_CAPTURE_MS,
    status = MediaStatus.PENDING,
)

fun sampleItems(n: Int): List<MediaItem> = (1..n).map { sampleItem(id = it.toString()) }

/** Stand-in for the live ExoPlayer surface so screenshots render deterministically. */
@Composable
fun PlaceholderVideo(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(Color(0xFF2B2F36)),
        contentAlignment = Alignment.Center,
    ) {
        Text("▶ video", color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

package com.presto.mediamanager.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Review-feed video: loops the clip and, when it's the active page, overlays a
 * [FeedScrubber] so you can scrub through before deciding. Only the on-screen
 * (playing) video advances; others pause and reset.
 */
@OptIn(UnstableApi::class)
@Composable
fun FeedPlayer(
    uri: String,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = playing
            prepare()
        }
    }

    var positionMs by remember(uri) { mutableLongStateOf(0L) }
    var durationMs by remember(uri) { mutableLongStateOf(0L) }
    var scrubbing by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(player, playing) {
        player.playWhenReady = playing
        if (!playing) {
            player.seekTo(0)
            return@LaunchedEffect
        }
        while (true) {
            if (!scrubbing) {
                positionMs = player.currentPosition
                durationMs = player.duration.coerceAtLeast(0L)
            }
            delay(200)
        }
    }

    DisposableEffect(uri) { onDispose { player.release() } }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
        )

        if (playing) {
            FeedScrubber(
                positionMs = positionMs,
                durationMs = durationMs,
                onScrubStart = { scrubbing = true },
                onSeek = { ms -> positionMs = ms; player.seekTo(ms) },
                onScrubEnd = { scrubbing = false },
                // Sit above the action bar.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 104.dp),
            )
        }
    }
}

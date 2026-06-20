package com.presto.mediamanager.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/** A muted-or-not ExoPlayer surface that loops the given video forever. */
@Composable
fun LoopingVideoPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
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

    // Only the on-screen video should play; pause the rest so a big feed stays smooth.
    LaunchedEffect(player, playing) {
        player.playWhenReady = playing
        if (!playing) player.seekTo(0)
    }

    DisposableEffect(uri) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = player
            }
        },
    )
}

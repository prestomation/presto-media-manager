package com.presto.mediamanager.ui.components

import android.net.Uri
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Editor preview: plays the source on a loop **restricted to the current trim
 * window**, so the user previews exactly what will be exported. When
 * [zoomEnabled] is on, pinch-to-zoom and pan let them inspect detail.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun EditorPreviewPlayer(
    uri: String,
    trimStartMs: Long,
    trimEndMs: Long,
    zoomEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            playWhenReady = true
            prepare()
        }
    }

    val start by rememberUpdatedState(trimStartMs)
    val end by rememberUpdatedState(trimEndMs)

    // Seek to the trim start whenever it changes, then keep playback inside [start, end].
    LaunchedEffect(player, trimStartMs) { player.seekTo(start) }
    LaunchedEffect(player) {
        while (true) {
            val pos = player.currentPosition
            if (end > start && (pos >= end - 40 || pos < start - 40)) {
                player.seekTo(start)
            }
            delay(100)
        }
    }

    DisposableEffect(uri) { onDispose { player.release() } }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(zoomEnabled) {
        if (!zoomEnabled) { scale = 1f; offset = Offset.Zero }
    }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }

    Box(
        modifier
            .then(if (zoomEnabled) Modifier.transformable(transformState) else Modifier)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            ),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
        )
    }
}

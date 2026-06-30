package com.presto.mediamanager.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.media.CropRect
import com.presto.mediamanager.media.TrimRange
import com.presto.mediamanager.ui.components.EditorPreviewPlayer
import java.util.Locale

@Composable
fun EditorScreen(
    uri: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    LaunchedEffect(uri) { viewModel.load(uri) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.finished) { if (state.finished) onDone() }

    EditorContent(
        state = state,
        onBack = onBack,
        onTrimChange = viewModel::setTrim,
        onScrubChange = viewModel::setScrub,
        onCropChange = viewModel::setCrop,
        onRemoveAudioChange = viewModel::setRemoveAudio,
        onShareResolutionChange = viewModel::setShareResolution,
        onExport = viewModel::export,
        videoSlot = { item, zoomEnabled ->
            EditorPreviewPlayer(
                uri = item.uri,
                trimStartMs = state.trim.startMs,
                trimEndMs = state.trim.endMs,
                zoomEnabled = zoomEnabled,
                scrubMs = state.scrubMs,
                modifier = Modifier.fillMaxSize(),
                onPosition = viewModel::setPosition,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorContent(
    state: EditorUiState,
    onBack: () -> Unit,
    onTrimChange: (TrimRange) -> Unit,
    onScrubChange: (Long?) -> Unit = {},
    onCropChange: (CropRect) -> Unit,
    onRemoveAudioChange: (Boolean) -> Unit,
    onShareResolutionChange: (com.presto.mediamanager.data.settings.ShareResolution) -> Unit,
    onExport: (String, com.presto.mediamanager.media.ExportDestination) -> Unit,
    videoSlot: @Composable (MediaItem, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    var zoomInspect by remember { mutableStateOf(false) }
    var aspectLocked by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Edit clip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { aspectLocked = !aspectLocked }, enabled = !zoomInspect) {
                        Icon(
                            if (aspectLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (aspectLocked) "Unlock aspect ratio" else "Lock aspect ratio",
                            tint = if (aspectLocked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                    IconButton(onClick = { zoomInspect = !zoomInspect }) {
                        Icon(
                            if (zoomInspect) Icons.Filled.ZoomOut else Icons.Filled.ZoomIn,
                            contentDescription = "Toggle zoom inspect",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(state.trim.startMs), style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Clip: ${formatTime(state.trim.endMs - state.trim.startMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(formatTime(state.trim.endMs), style = MaterialTheme.typography.labelMedium)
                }
                TrimBar(
                    durationMs = state.durationMs,
                    trim = state.trim,
                    positionMs = state.positionMs,
                    thumbnails = state.thumbnails,
                    onTrimChange = onTrimChange,
                    onScrub = onScrubChange,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = { showSheet = true },
                    enabled = !state.exporting,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(if (state.exporting) "Exporting…" else "Save")
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
                // Keep edge swipes over the crop area from triggering system back.
                .systemGestureExclusion(),
            contentAlignment = Alignment.Center,
        ) {
            val item = state.item
            if (item == null) {
                CircularProgressIndicator()
            } else {
                videoSlot(item, zoomInspect)
                if (!zoomInspect) {
                    CropOverlay(
                        crop = state.crop,
                        onCropChange = onCropChange,
                        aspectLocked = aspectLocked,
                    )
                }
                if (state.exporting) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    )
                }
            }
        }
    }

    if (showSheet) {
        ExportSheet(
            state = state,
            onRemoveAudioChange = onRemoveAudioChange,
            onShareResolutionChange = onShareResolutionChange,
            onExport = { label, dest ->
                showSheet = false
                onExport(label, dest)
            },
            onDismiss = { showSheet = false },
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

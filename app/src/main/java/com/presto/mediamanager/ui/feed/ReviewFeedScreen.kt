package com.presto.mediamanager.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.data.settings.PlaybackSpeed
import com.presto.mediamanager.ui.components.FeedPlayer
import com.presto.mediamanager.ui.components.LabelDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Long-press threshold for resetting the speed pill to the configured default. */
private const val SPEED_RESET_HOLD_MS = 1000L

@Composable
fun ReviewFeedScreen(
    onOpenSettings: () -> Unit,
    onReview: (MediaItem) -> Unit,
    viewModel: ReviewFeedViewModel = viewModel(),
) {
    val items by viewModel.queue.collectAsState()
    val configured by viewModel.configured.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(Unit) {
        viewModel.undoEvents.collect { item ->
            val result = snackbarHostState.showSnackbar(
                message = "Video deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(item)
                // The restored item slides back into its original spot in the queue,
                // which may be above where the user has since scrolled. Wait for it to
                // reappear, then bring the feed back to it instead of stranding the
                // user on whatever video they were on.
                val index = snapshotFlow { items.indexOfFirst { it.uri == item.uri } }
                    .first { it >= 0 }
                pagerState.animateScrollToPage(index)
            } else {
                viewModel.commitDelete(item)
            }
        }
    }

    ReviewFeedContent(
        items = items,
        configured = configured,
        snackbarHostState = snackbarHostState,
        pagerState = pagerState,
        onOpenSettings = onOpenSettings,
        onDelete = viewModel::delete,
        onLater = viewModel::later,
        onArchive = viewModel::archive,
        onReview = onReview,
        currentSpeed = currentSpeed,
        onCycleSpeed = viewModel::cycleSpeed,
        onResetSpeed = viewModel::resetSpeed,
        videoSlot = { item, active ->
            FeedPlayer(
                uri = item.uri,
                modifier = Modifier.fillMaxSize(),
                playing = active,
                speed = currentSpeed.multiplier,
            )
        },
    )
}

/** Stateless feed body — rendered directly by screenshot tests. */
@Composable
fun ReviewFeedContent(
    items: List<MediaItem>,
    onOpenSettings: () -> Unit,
    onDelete: (MediaItem) -> Unit,
    onLater: (MediaItem) -> Unit,
    onArchive: (MediaItem, String) -> Unit,
    onReview: (MediaItem) -> Unit,
    videoSlot: @Composable (MediaItem, Boolean) -> Unit,
    configured: Boolean = true,
    currentSpeed: PlaybackSpeed = PlaybackSpeed.X1_5,
    onCycleSpeed: () -> Unit = {},
    onResetSpeed: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    pagerState: PagerState = rememberPagerState(pageCount = { items.size }),
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyFeed(onOpenSettings, configured, modifier)
        return
    }

    val scope = rememberCoroutineScope()
    var labelTarget by remember { mutableStateOf<MediaItem?>(null) }

    // Later keeps the item in the queue, so step forward to the next one.
    // Delete/Archive remove the item, so the next one slides into place on its own.
    fun advance() {
        val next = (pagerState.currentPage + 1).coerceAtMost(items.lastIndex)
        scope.launch { pagerState.animateScrollToPage(next) }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            key = { items[it].uri },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page]
            Box(Modifier.fillMaxSize()) {
                videoSlot(item, page == pagerState.currentPage)

                FeedTopBar(
                    position = page + 1,
                    total = items.size,
                    item = item,
                    onOpenSettings = onOpenSettings,
                    speed = currentSpeed,
                    onCycleSpeed = onCycleSpeed,
                    onResetSpeed = onResetSpeed,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                FeedActionBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onDelete = { onDelete(item) },
                    onLater = { onLater(item); advance() },
                    onArchive = { labelTarget = item },
                    onReview = { onReview(item) },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
    }

    labelTarget?.let { target ->
        LabelDialog(
            title = "Archive clip",
            confirmText = "Archive",
            onConfirm = { label ->
                onArchive(target, label)
                labelTarget = null
            },
            onDismiss = { labelTarget = null },
        )
    }
}

@Composable
private fun FeedTopBar(
    position: Int,
    total: Int,
    item: MediaItem,
    onOpenSettings: () -> Unit,
    speed: PlaybackSpeed,
    onCycleSpeed: () -> Unit,
    onResetSpeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "$position / $total to review",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${item.displayName} · ${formatDate(item.dateCapturedMs)}",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SpeedPill(speed = speed, onTap = onCycleSpeed, onLongPress = onResetSpeed)
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
        }
    }
}

/**
 * Tap cycles through the playback speeds; holding for [SPEED_RESET_HOLD_MS]
 * snaps back to the configured default. Deliberately out of the primary
 * thumb path — it's tucked next to Settings since it's rarely touched.
 */
@Composable
private fun SpeedPill(
    speed: PlaybackSpeed,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
            .pointerInput(onTap, onLongPress) {
                while (true) {
                    awaitPointerEventScope { awaitFirstDown() }
                    val releasedInTime = withTimeoutOrNull(SPEED_RESET_HOLD_MS) {
                        awaitPointerEventScope { waitForRelease() }
                    } != null
                    if (releasedInTime) {
                        onTap()
                    } else {
                        onLongPress()
                        awaitPointerEventScope { waitForRelease() }
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "Playback speed ${speed.label}"
                onClick(label = "next speed") { onTap(); true }
                onLongClick(label = "reset to default speed") { onLongPress(); true }
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            speed.label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Suspends until every active pointer has been released. */
private suspend fun AwaitPointerEventScope.waitForRelease() {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.all { !it.pressed }) return
    }
}

@Composable
private fun FeedActionBar(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onLater: () -> Unit,
    onArchive: () -> Unit,
    onReview: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedAction("Delete", Icons.Filled.Delete, MaterialTheme.colorScheme.error, onDelete)
        FeedAction("Later", Icons.Filled.Schedule, Color(0xFFFFC857), onLater)
        FeedAction("Archive", Icons.Filled.Archive, Color(0xFF7FD8A0), onArchive)
        FeedAction("Review", Icons.Filled.ContentCut, MaterialTheme.colorScheme.primary, onReview)
    }
}

@Composable
private fun FeedAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    // One merged, labeled clickable: the visible text is the single speakable
    // label (no icon/label duplication) and the whole column is the touch target.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick, role = Role.Button)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyFeed(
    onOpenSettings: () -> Unit,
    configured: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            if (configured) {
                Text("All caught up", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "No videos waiting for review.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                IconButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            } else {
                Text("Set up your folders", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Choose the input, archive, and share folders to start reviewing.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 16.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text("Open settings", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))

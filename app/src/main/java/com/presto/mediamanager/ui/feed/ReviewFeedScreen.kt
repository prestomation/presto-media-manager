package com.presto.mediamanager.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.ui.components.FeedPlayer
import com.presto.mediamanager.ui.components.LabelDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewFeedScreen(
    onOpenSettings: () -> Unit,
    onReview: (MediaItem) -> Unit,
    viewModel: ReviewFeedViewModel = viewModel(),
) {
    val items by viewModel.queue.collectAsState()
    val configured by viewModel.configured.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.undoEvents.collect { item ->
            val result = snackbarHostState.showSnackbar(
                message = "Video deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(item)
            } else {
                viewModel.commitDelete(item)
            }
        }
    }

    ReviewFeedContent(
        items = items,
        configured = configured,
        snackbarHostState = snackbarHostState,
        onOpenSettings = onOpenSettings,
        onDelete = viewModel::delete,
        onLater = viewModel::later,
        onArchive = viewModel::archive,
        onReview = onReview,
        videoSlot = { item, active ->
            FeedPlayer(uri = item.uri, modifier = Modifier.fillMaxSize(), playing = active)
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyFeed(onOpenSettings, configured, modifier)
        return
    }

    val pagerState = rememberPagerState(pageCount = { items.size })
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

        // Anchored to the top so it never covers the scrub bar / action bar below.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
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
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
        }
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

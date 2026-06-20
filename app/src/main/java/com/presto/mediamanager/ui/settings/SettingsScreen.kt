package com.presto.mediamanager.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.presto.mediamanager.BuildConfig
import com.presto.mediamanager.data.settings.AppSettings
import com.presto.mediamanager.data.settings.ShareResolution

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val buildInfo = "v${BuildConfig.VERSION_NAME} · ${BuildConfig.GIT_SHA} · ${BuildConfig.BUILD_TIME} UTC"
    SettingsContent(
        state = state,
        buildInfo = buildInfo,
        onBack = onBack,
        onFolderPicked = viewModel::onFolderPicked,
        onAutoDeleteEnabled = viewModel::setAutoDeleteEnabled,
        onAutoDeleteDays = viewModel::setAutoDeleteDays,
        onDefaultRemoveAudio = viewModel::setDefaultRemoveAudio,
        onDefaultShareResolution = viewModel::setDefaultShareResolution,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: AppSettings,
    onBack: () -> Unit,
    onFolderPicked: (FolderKind, Uri) -> Unit,
    onAutoDeleteEnabled: (Boolean) -> Unit,
    onAutoDeleteDays: (Int) -> Unit,
    onDefaultRemoveAudio: (Boolean) -> Unit,
    onDefaultShareResolution: (ShareResolution) -> Unit,
    buildInfo: String = "",
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle("Folders")
            FolderRow("Input (watch)", state.inputFolderUri, FolderKind.INPUT, onFolderPicked)
            FolderRow("Archive (full-res)", state.archiveFolderUri, FolderKind.ARCHIVE, onFolderPicked)
            FolderRow("Share (downscaled)", state.shareFolderUri, FolderKind.SHARE, onFolderPicked)

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            SectionTitle("Auto-delete")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Auto-delete unreviewed videos", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.autoDeleteEnabled, onCheckedChange = onAutoDeleteEnabled)
            }
            if (state.autoDeleteEnabled) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("After ${state.autoDeleteDays} days", style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onAutoDeleteDays(state.autoDeleteDays - 1) }) { Text("−") }
                        Text(
                            "${state.autoDeleteDays}",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButton(onClick = { onAutoDeleteDays(state.autoDeleteDays + 1) }) { Text("+") }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            SectionTitle("Export defaults")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Remove audio by default", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.defaultRemoveAudio, onCheckedChange = onDefaultRemoveAudio)
            }
            Text(
                "Default share resolution",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareResolution.entries.forEach { res ->
                    FilterChip(
                        selected = state.defaultShareResolution == res,
                        onClick = { onDefaultShareResolution(res) },
                        label = { Text(res.label) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

            SectionTitle("About")
            Text(
                buildInfo.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun FolderRow(
    title: String,
    uri: String?,
    kind: FolderKind,
    onFolderPicked: (FolderKind, Uri) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { picked -> if (picked != null) onFolderPicked(kind, picked) }

    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    uri?.let { prettyTreePath(it) } ?: "Not set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val action = if (uri == null) "Choose" else "Change"
            OutlinedButton(
                onClick = { launcher.launch(null) },
                // Unique speakable label per row so screen readers can tell them apart.
                modifier = Modifier.semantics { contentDescription = "$action $title folder" },
            ) {
                Text(action)
            }
        }
    }
}

/** Render a SAF tree URI as something human-readable-ish. */
private fun prettyTreePath(uri: String): String =
    Uri.decode(uri.substringAfterLast("/tree/").ifBlank { uri })

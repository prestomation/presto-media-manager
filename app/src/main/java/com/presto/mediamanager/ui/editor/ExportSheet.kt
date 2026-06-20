package com.presto.mediamanager.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.presto.mediamanager.data.settings.ShareResolution
import com.presto.mediamanager.media.ExportDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    state: EditorUiState,
    onRemoveAudioChange: (Boolean) -> Unit,
    onShareResolutionChange: (ShareResolution) -> Unit,
    onExport: (String, ExportDestination) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Save clip", style = MaterialTheme.typography.titleLarge)
            Text(
                "Full-resolution copy goes to Archive. Sharing also saves a downscaled copy.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                singleLine = true,
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Remove audio", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.removeAudio, onCheckedChange = onRemoveAudioChange)
            }

            Text(
                "Share resolution",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareResolution.entries.forEach { res ->
                    FilterChip(
                        selected = state.shareResolution == res,
                        onClick = { onShareResolutionChange(res) },
                        label = { Text(res.label) },
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onExport(label, ExportDestination.ARCHIVE) },
                    enabled = label.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null)
                    Text("Archive", modifier = Modifier.padding(start = 8.dp))
                }
                Button(
                    onClick = { onExport(label, ExportDestination.SHARE) },
                    enabled = label.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("Share + Archive", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

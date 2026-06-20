package com.presto.mediamanager.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.presto.mediamanager.PrestoApp
import com.presto.mediamanager.data.settings.AppSettings
import com.presto.mediamanager.data.settings.ShareResolution
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FolderKind { INPUT, ARCHIVE, SHARE }

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as PrestoApp).container
    private val settings = container.settingsRepository
    private val saf = container.safManager

    val state: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun onFolderPicked(kind: FolderKind, uri: Uri) {
        saf.persistTreePermission(uri)
        viewModelScope.launch {
            when (kind) {
                FolderKind.INPUT -> settings.setInputFolder(uri.toString())
                FolderKind.ARCHIVE -> settings.setArchiveFolder(uri.toString())
                FolderKind.SHARE -> settings.setShareFolder(uri.toString())
            }
        }
    }

    fun setAutoDeleteEnabled(enabled: Boolean) = launch { settings.setAutoDeleteEnabled(enabled) }
    fun setAutoDeleteDays(days: Int) = launch { settings.setAutoDeleteDays(days) }
    fun setDefaultRemoveAudio(value: Boolean) = launch { settings.setDefaultRemoveAudio(value) }
    fun setDefaultShareResolution(res: ShareResolution) = launch { settings.setDefaultShareResolution(res) }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}

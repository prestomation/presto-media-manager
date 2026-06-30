package com.presto.mediamanager.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.presto.mediamanager.PrestoApp
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.data.settings.ShareResolution
import com.presto.mediamanager.media.CropRect
import com.presto.mediamanager.media.ExportDestination
import com.presto.mediamanager.media.ExportRequest
import com.presto.mediamanager.media.TrimRange
import com.presto.mediamanager.util.VideoThumbnails
import com.presto.mediamanager.work.BadgeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val item: MediaItem? = null,
    val durationMs: Long = 0,
    val trim: TrimRange = TrimRange(0, 0),
    val crop: CropRect = CropRect(),
    val positionMs: Long = 0,
    /** Non-null while a trim handle is being dragged: the frame to hold for preview. */
    val scrubMs: Long? = null,
    val thumbnails: List<androidx.compose.ui.graphics.ImageBitmap> = emptyList(),
    val removeAudio: Boolean = false,
    val shareResolution: ShareResolution = ShareResolution.P720,
    val exporting: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null,
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as PrestoApp).container
    private val repo = container.mediaRepository

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    fun load(uri: String) {
        if (_state.value.item?.uri == uri) return
        viewModelScope.launch {
            val found = repo.findForEditing(uri) ?: return@launch
            val settings = container.settingsRepository.current()
            _state.update {
                it.copy(
                    item = found,
                    durationMs = found.durationMs,
                    trim = TrimRange(0, found.durationMs),
                    removeAudio = settings.defaultRemoveAudio,
                    shareResolution = settings.defaultShareResolution,
                )
            }
            val frames = VideoThumbnails.extract(getApplication(), found.uri, found.durationMs)
            _state.update { it.copy(thumbnails = frames) }
        }
    }

    fun setPosition(ms: Long) = _state.update { it.copy(positionMs = ms) }
    fun setScrub(ms: Long?) = _state.update { it.copy(scrubMs = ms) }
    fun setTrim(trim: TrimRange) = _state.update { it.copy(trim = trim) }
    fun setCrop(crop: CropRect) = _state.update { it.copy(crop = crop) }
    fun setRemoveAudio(value: Boolean) = _state.update { it.copy(removeAudio = value) }
    fun setShareResolution(res: ShareResolution) = _state.update { it.copy(shareResolution = res) }

    fun export(label: String, destination: ExportDestination) {
        val s = _state.value
        val item = s.item ?: return
        _state.update { it.copy(exporting = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repo.exportEdited(
                    item,
                    ExportRequest(
                        label = label,
                        trim = s.trim,
                        crop = s.crop,
                        removeAudio = s.removeAudio,
                        shareResolution = s.shareResolution,
                        destination = destination,
                    ),
                )
            }.onSuccess {
                BadgeManager.update(getApplication(), repo.reviewCount())
                _state.update { it.copy(exporting = false, finished = true) }
            }.onFailure { e ->
                _state.update { it.copy(exporting = false, error = e.message ?: "Export failed") }
            }
        }
    }
}

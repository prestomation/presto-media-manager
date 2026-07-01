package com.presto.mediamanager.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.presto.mediamanager.PrestoApp
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.data.settings.PlaybackSpeed
import com.presto.mediamanager.work.BadgeManager
import com.presto.mediamanager.work.WorkScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReviewFeedViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as PrestoApp).container
    private val repo = container.mediaRepository

    /** URIs whose deletion is deferred (hidden from the feed, file not yet removed). */
    private val pendingDeletes = MutableStateFlow<Set<String>>(emptySet())

    /** Emits the item just soft-deleted so the UI can offer Undo. */
    private val _undoEvents = MutableSharedFlow<MediaItem>(extraBufferCapacity = 1)
    val undoEvents = _undoEvents.asSharedFlow()

    val queue: StateFlow<List<MediaItem>> =
        combine(repo.observeReviewQueue(), pendingDeletes) { items, hidden ->
            items.filterNot { it.uri in hidden }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val configured: StateFlow<Boolean> = container.settingsRepository.settings
        .map { it.isConfigured }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val defaultSpeed: StateFlow<PlaybackSpeed> = container.settingsRepository.settings
        .map { it.defaultPlaybackSpeed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackSpeed.X1_5)

    /** Session-local override of the default speed; null while un-touched. */
    private val speedOverride = MutableStateFlow<PlaybackSpeed?>(null)

    val currentSpeed: StateFlow<PlaybackSpeed> = combine(speedOverride, defaultSpeed) { override, default ->
        override ?: default
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackSpeed.X1_5)

    init {
        refresh()
    }

    /** Tap: step to the next speed in the fixed cycle. */
    fun cycleSpeed() {
        val entries = PlaybackSpeed.entries
        val next = entries[(entries.indexOf(currentSpeed.value) + 1) % entries.size]
        speedOverride.value = next
    }

    /** Long-press: drop any session override and fall back to the configured default. */
    fun resetSpeed() {
        speedOverride.value = null
    }

    fun refresh() {
        WorkScheduler.scanNow(getApplication())
    }

    /**
     * Soft-delete: hide the item immediately and emit an Undo event. The real
     * file deletion is deferred to [commitDelete], called when the Undo snackbar
     * is dismissed. If the process dies first, nothing is lost — the row stays
     * PENDING and the video reappears on the next scan.
     */
    fun delete(item: MediaItem) {
        pendingDeletes.update { it + item.uri }
        _undoEvents.tryEmit(item)
    }

    fun undoDelete(item: MediaItem) {
        pendingDeletes.update { it - item.uri }
    }

    fun commitDelete(item: MediaItem) = run {
        repo.delete(item)
        pendingDeletes.update { it - item.uri }
    }

    fun later(item: MediaItem) = run { repo.markLater(item) }
    fun archive(item: MediaItem, label: String) = run { repo.quickArchive(item, label) }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            BadgeManager.update(getApplication(), repo.reviewCount())
        }
    }
}

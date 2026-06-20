package com.presto.mediamanager.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.presto.mediamanager.PrestoApp
import com.presto.mediamanager.data.db.MediaItem
import com.presto.mediamanager.work.BadgeManager
import com.presto.mediamanager.work.WorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewFeedUiState(
    val items: List<MediaItem> = emptyList(),
    val loading: Boolean = true,
)

class ReviewFeedViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as PrestoApp).container
    private val repo = container.mediaRepository

    val queue: StateFlow<List<MediaItem>> = repo.observeReviewQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        WorkScheduler.scanNow(getApplication())
    }

    fun delete(item: MediaItem) = run { repo.delete(item) }
    fun later(item: MediaItem) = run { repo.markLater(item) }
    fun archive(item: MediaItem, label: String) = run { repo.quickArchive(item, label) }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            BadgeManager.update(getApplication(), repo.reviewCount())
        }
    }
}

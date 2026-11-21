package com.nahueltaibo.pagecast.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nahueltaibo.pagecast.data.Library
import com.nahueltaibo.pagecast.data.LocalEpisode
import com.nahueltaibo.pagecast.data.RemoteEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A row in the library: what the feed offers, what's on the phone, or both. */
data class EpisodeRow(val id: String, val title: String, val remote: RemoteEpisode?, val local: LocalEpisode?) {
    val source get() = remote?.source ?: local?.presentation?.source
    val categories get() = remote?.categories?.takeIf { it.isNotEmpty() } ?: local?.presentation?.categories.orEmpty()
}

data class LibraryState(
    val feeds: List<String> = emptyList(),
    val rows: List<EpisodeRow> = emptyList(),
    val downloads: Map<String, Float> = emptyMap(),
    val refreshing: Boolean = false,
    val error: String? = null,
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    val library = Library(app)
    private val remote = MutableStateFlow<List<RemoteEpisode>>(emptyList())
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null, feeds = library.feeds()) }
            val errors = mutableListOf<String>()
            remote.value = library.feeds().flatMap { url ->
                runCatching { library.fetchFeed(url) }.getOrElse { errors += "$url: ${it.message}"; emptyList() }
            }
            _state.update { it.copy(refreshing = false, error = errors.firstOrNull()) }
            rebuildRows()
        }
    }

    fun addFeed(url: String) {
        if (url.isBlank()) return
        library.addFeed(url)
        refresh()
    }

    fun download(episode: RemoteEpisode) {
        if (episode.id in _state.value.downloads) return
        viewModelScope.launch {
            _state.update { it.copy(downloads = it.downloads + (episode.id to 0f)) }
            runCatching {
                library.download(episode) { p -> _state.update { it.copy(downloads = it.downloads + (episode.id to p)) } }
            }.onFailure { e -> _state.update { it.copy(error = "Download failed: ${e.message}") } }
            _state.update { it.copy(downloads = it.downloads - episode.id) }
            rebuildRows()
        }
    }

    fun delete(episode: LocalEpisode) {
        library.delete(episode)
        rebuildRows()
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun rebuildRows() {
        val local = library.downloaded().associateBy { it.id }
        val fromFeeds = remote.value.map { EpisodeRow(it.id, it.title, it, local[it.id]) }
        val onlyLocal = local.values.filter { l -> fromFeeds.none { it.id == l.id } }
            .map { EpisodeRow(it.id, it.presentation.title, null, it) }
        _state.update { it.copy(rows = fromFeeds + onlyLocal) }
    }
}

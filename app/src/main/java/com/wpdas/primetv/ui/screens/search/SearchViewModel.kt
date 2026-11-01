package com.wpdas.primetv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.data.repository.toMediaItem
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val suggestions: List<MediaItem> = emptyList(),
    val results: List<MediaItem> = emptyList(),
    val searched: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        query.debounce(DEBOUNCE_MS)
            .mapLatest { q ->
                if (q.isBlank()) return@mapLatest emptyList<MediaItem>() to false
                val r = catalog.search(q, MAX_RESULTS)
                val items = r.movies.map { it.toMediaItem() } + r.series.map { it.toMediaItem() } + r.channels.map { it.toMediaItem() }
                items to true
            }
            .onEach { (items, searched) ->
                _state.update { it.copy(suggestions = items.take(SUGGESTIONS), results = items, searched = searched) }
            }
            .launchIn(viewModelScope)
    }

    fun append(text: String) = setQuery(_state.value.query + text)
    fun space() { if (_state.value.query.isNotEmpty() && !_state.value.query.endsWith(' ')) setQuery(_state.value.query + " ") }
    fun backspace() = setQuery(_state.value.query.dropLast(1))
    fun clear() = setQuery("")

    fun setQuery(text: String) {
        val q = text.take(MAX_QUERY)
        query.value = q
        _state.update { it.copy(query = q, searched = if (q.isBlank()) false else it.searched) }
    }

    private companion object {
        const val DEBOUNCE_MS = 80L
        const val MAX_RESULTS = 60
        const val SUGGESTIONS = 5
        const val MAX_QUERY = 40
    }
}

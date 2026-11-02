package com.wpdas.primetv.ui.screens.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.repository.HomeRepositoryImpl
import com.wpdas.primetv.data.repository.toMediaItem
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.ui.navigation.CategoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class CategorySort(val labelKey: String) { RECENT("category.sort_recent"), AZ("category.sort_az"), YEAR("category.sort_year") }

data class CategoryUiState(
    val title: String,
    /** null enquanto carrega. */
    val items: List<MediaItem>? = null,
    val total: Int = 0,
    val sort: CategorySort = CategorySort.RECENT,
    val query: String = "",
    /** Gêneros presentes nos itens (séries trazem gênero na lista; filmes normalmente não). */
    val genres: List<String> = emptyList(),
    val genre: String? = null,
    val isLive: Boolean = false,
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CategoryRoute>()
    private val section = ContentSection.entries.firstOrNull { it.name == route.section } ?: ContentSection.MOVIES

    private val sort = MutableStateFlow(if (section == ContentSection.LIVE) CategorySort.AZ else CategorySort.RECENT)
    private val query = MutableStateFlow("")
    private val genre = MutableStateFlow<String?>(null)

    private val source = when (section) {
        ContentSection.LIVE -> catalog.channels(
            if (route.categoryId == HomeRepositoryImpl.FOOTBALL_CATEGORY) ChannelFilter.Football
            else route.categoryId?.let { ChannelFilter.Category(it) } ?: ChannelFilter.All,
        ).map { list -> list.map { it.toMediaItem() } }
        ContentSection.MOVIES -> catalog.movies(route.categoryId, Int.MAX_VALUE).map { list -> list.map { it.toMediaItem() } }
        else -> catalog.series(route.categoryId, Int.MAX_VALUE).map { list -> list.map { it.toMediaItem() } }
    }

    val state: StateFlow<CategoryUiState> = combine(source, sort, query, genre) { all, sort, q, g ->
        val genres = all.flatMap { it.genres }.map { it.trim() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().filter { it.value >= 3 }.keys.sorted()
        val needle = TextNormalizer.searchKey(q)
        var items = all
        if (g != null) items = items.filter { item -> item.genres.any { it.trim() == g } }
        if (needle.isNotEmpty()) items = items.filter { TextNormalizer.searchKey(it.title).contains(needle) }
        items = when (sort) {
            CategorySort.RECENT -> items // ordem da consulta (adicionados recentemente / número do canal)
            CategorySort.AZ -> items.sortedBy { TextNormalizer.searchKey(it.title) }
            CategorySort.YEAR -> items.sortedWith(compareByDescending<MediaItem> { it.year ?: 0 }.thenBy { TextNormalizer.searchKey(it.title) })
        }
        CategoryUiState(
            title = route.title,
            items = items,
            total = all.size,
            sort = sort,
            query = q,
            genres = if (section == ContentSection.LIVE || genres.size <= 1) emptyList() else genres,
            genre = g,
            isLive = section == ContentSection.LIVE,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState(route.title, isLive = section == ContentSection.LIVE))

    fun setSort(value: CategorySort) = sort.update { value }
    fun setQuery(text: String) = query.update { text.take(40) }
    fun setGenre(value: String?) = genre.update { if (it == value) null else value }
}

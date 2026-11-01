package com.wpdas.primetv.ui.screens.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wpdas.primetv.data.repository.HomeRepositoryImpl
import com.wpdas.primetv.data.repository.toMediaItem
import com.wpdas.primetv.domain.model.ContentSection
import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.repository.CatalogRepository
import com.wpdas.primetv.domain.repository.ChannelFilter
import com.wpdas.primetv.ui.navigation.CategoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryUiState(val title: String, val items: List<MediaItem>? = null)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalog: CatalogRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CategoryRoute>()
    private val section = ContentSection.entries.firstOrNull { it.name == route.section } ?: ContentSection.MOVIES

    val state: StateFlow<CategoryUiState> = when (section) {
        ContentSection.LIVE -> catalog.channels(
            if (route.categoryId == HomeRepositoryImpl.FOOTBALL_CATEGORY) ChannelFilter.Football
            else route.categoryId?.let { ChannelFilter.Category(it) } ?: ChannelFilter.All,
        ).map { list -> list.map { it.toMediaItem() } }
        ContentSection.MOVIES -> catalog.movies(route.categoryId, Int.MAX_VALUE).map { list -> list.map { it.toMediaItem() } }
        else -> catalog.series(route.categoryId, Int.MAX_VALUE).map { list -> list.map { it.toMediaItem() } }
    }
        .map { CategoryUiState(route.title, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState(route.title))
}

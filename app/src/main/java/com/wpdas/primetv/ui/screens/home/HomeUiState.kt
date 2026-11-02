package com.wpdas.primetv.ui.screens.home

import com.wpdas.primetv.domain.model.CategoryRow
import com.wpdas.primetv.domain.model.ContinueWatchingItem
import com.wpdas.primetv.domain.model.FootballEvent
import com.wpdas.primetv.domain.model.HomeTab
import com.wpdas.primetv.domain.model.MediaItem

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.HOME,
    /** Abas visíveis na barra (Kids/Anime só com conteúdo). */
    val tabs: List<HomeTab> = HomeTab.entries.filter { it !in HomeTab.OPTIONAL },
    val featured: List<MediaItem> = emptyList(),
    val featuredIndex: Int = 0,
    /** Item fixado no destaque porque um card dele está em foco (sobrepõe a rotação). */
    val pinnedHero: MediaItem? = null,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val football: List<FootballEvent> = emptyList(),
    val rows: List<CategoryRow> = emptyList(),
    /** Chave i18n da seção em atualização (ex.: "section.movies"), ou null. */
    val updatingSectionKey: String? = null,
    /** Versão nova do app para o aviso discreto, ou null. */
    val updateVersion: String? = null,
) {
    val hero: MediaItem? get() = pinnedHero ?: featured.getOrNull(featuredIndex)
    val showDots: Boolean get() = pinnedHero == null && featured.size > 1
}

package com.wpdas.primetv.ui.navigation

import kotlinx.serialization.Serializable

/** Destinos tipados do Navigation Compose. */
@Serializable data class HomeRoute(val tab: String? = null)
@Serializable data object SearchRoute
@Serializable data class DetailsRoute(val itemId: String)
@Serializable data object LiveRoute
/** Grade completa de uma categoria. [categoryId] null = todos; "football" = só futebol. */
@Serializable data class CategoryRoute(val section: String, val categoryId: String?, val title: String)
@Serializable data class PlayerRoute(val itemId: String, val episodeLabel: String? = null)
@Serializable data object AppMenuRoute
@Serializable data object SettingsRoute
@Serializable data object SetupRoute
@Serializable data object ReceiveLinkRoute

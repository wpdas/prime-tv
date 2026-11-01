package com.wpdas.primetv.ui.navigation

import kotlinx.serialization.Serializable

/** Destinos tipados do Navigation Compose. */
@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data class DetailsRoute(val itemId: String)
@Serializable data object LiveRoute
@Serializable data class PlayerRoute(val itemId: String, val episodeLabel: String? = null)
@Serializable data object AppMenuRoute
@Serializable data object SettingsRoute
@Serializable data object SetupRoute
@Serializable data object ReceiveLinkRoute

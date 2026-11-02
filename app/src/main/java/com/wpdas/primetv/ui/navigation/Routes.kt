package com.wpdas.primetv.ui.navigation

import kotlinx.serialization.Serializable

/** Destinos tipados do Navigation Compose. */
@Serializable data class HomeRoute(val tab: String? = null)
@Serializable data object SearchRoute
@Serializable data class DetailsRoute(val itemId: String)
@Serializable data object LiveRoute
/** Guia de jogos (aba Futebol). */
@Serializable data object FootballRoute
/** Grade completa de uma categoria. [categoryId] null = todos; "football" = só futebol. */
@Serializable data class CategoryRoute(val section: String, val categoryId: String?, val title: String)
/** [itemId]: filme, série ou canal. [episodeId] escolhe o episódio; sem ele, a série retoma de onde parou. */
@Serializable data class PlayerRoute(val itemId: String, val episodeId: String? = null)
@Serializable data object AppMenuRoute
@Serializable data object AboutRoute
@Serializable data object UpdateRoute
@Serializable data object SettingsRoute
/** [prefill]: URL recebida pelo celular; o Setup conecta automaticamente. */
@Serializable data class SetupRoute(val prefill: String? = null)
@Serializable data object ReceiveLinkRoute

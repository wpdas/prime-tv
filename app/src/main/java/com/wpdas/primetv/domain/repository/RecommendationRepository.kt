package com.wpdas.primetv.domain.repository

import com.wpdas.primetv.domain.model.MediaItem
import com.wpdas.primetv.domain.model.TmdbInfo
import kotlinx.coroutines.flow.Flow

/** Contexto do destaque: geral (Início), infantil (aba Kids) ou anime (aba Anime). */
enum class FeaturedKind { GENERAL, KIDS, ANIME, NOVELAS }

/** Recomendações e enriquecimento via TMDB, sempre restritos ao que existe na lista do usuário. */
interface RecommendationRepository {
    /** false quando não há chave configurada: a Início usa itens recentes da própria lista. */
    val isAvailable: Boolean

    /** Em alta, populares e em cartaz (ou os populares do [kind]) que existem na lista, já com arte e sinopse do TMDB. */
    fun featured(limit: Int, kind: FeaturedKind = FeaturedKind.GENERAL): Flow<List<MediaItem>>

    /** Detalhes do TMDB para um filme/série da lista; null se não houver correspondência. */
    suspend fun enrich(item: MediaItem): TmdbInfo?
}

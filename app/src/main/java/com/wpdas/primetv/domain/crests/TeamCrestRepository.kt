package com.wpdas.primetv.domain.crests

/** Escudo de um time (URL de imagem de terceiros) ou null quando não há. */
interface TeamCrestRepository {
    suspend fun crestUrl(team: String): String?
}

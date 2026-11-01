package com.wpdas.primetv.data.sync

import androidx.room.withTransaction
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.domain.football.FootballClassifier
import javax.inject.Inject
import javax.inject.Singleton

/** Refaz a marcação de futebol de tudo que já está no banco quando as palavras-chave mudam. */
@Singleton
class FootballReclassifier @Inject constructor(private val db: PrimeDatabase) {

    suspend fun run(classifier: FootballClassifier) {
        val dao = db.catalogDao()
        db.withTransaction {
            dao.channelRefs().chunked(CHUNK).forEach { chunk ->
                chunk.forEach { dao.setChannelFootball(it.id, classifier.matches(it.name, it.categoryName)) }
            }
            dao.movieRefs().chunked(CHUNK).forEach { chunk ->
                chunk.forEach { dao.setMovieFootball(it.id, classifier.matches(it.name, it.categoryName)) }
            }
            dao.seriesRefs().chunked(CHUNK).forEach { chunk ->
                chunk.forEach { dao.setSeriesFootball(it.id, classifier.matches(it.name, it.categoryName)) }
            }
        }
    }

    private companion object {
        const val CHUNK = 500
    }
}

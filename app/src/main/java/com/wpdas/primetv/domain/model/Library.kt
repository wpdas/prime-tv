package com.wpdas.primetv.domain.model

/** Progresso de reprodução de um filme, episódio ou canal. */
data class WatchProgress(
    val itemId: String,
    val type: MediaType,
    val seriesId: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val finished: Boolean,
) {
    val fraction: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    companion object {
        /** Acima disto o item conta como assistido e sai de "Continuar assistindo". */
        const val FINISHED_FRACTION = 0.9f
    }
}

/** Onde retomar uma série: episódio e posição. */
data class SeriesResume(val episode: Episode, val positionMs: Long)

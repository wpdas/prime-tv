package com.wpdas.primetv.domain.model

/** Regras de "onde retomar" compartilhadas por Detalhes, Início e player. */
object ResumeLogic {
    /**
     * Dado o último episódio tocado de uma série, decide onde retomar:
     * em andamento -> ele mesmo na posição salva; concluído -> o seguinte do início;
     * concluído e era o último -> null (série terminada).
     */
    fun resume(latest: WatchProgress?, episodes: List<Episode>): SeriesResume? {
        if (latest == null) return episodes.firstOrNull()?.let { SeriesResume(it, 0L) }
        val index = episodes.indexOfFirst { it.id == latest.itemId }
        if (index < 0) return episodes.firstOrNull()?.let { SeriesResume(it, 0L) }
        return if (latest.finished) episodes.getOrNull(index + 1)?.let { SeriesResume(it, 0L) }
        else SeriesResume(episodes[index], latest.positionMs)
    }

    fun isFinished(positionMs: Long, durationMs: Long): Boolean =
        durationMs > 0 && positionMs.toFloat() / durationMs >= WatchProgress.FINISHED_FRACTION
}

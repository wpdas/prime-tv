package com.wpdas.primetv.ui.screens.player

enum class QualityKind { AUTO, TRACK, SOURCE }

/** Opção do menu Qualidade: [id] é a faixa ("gi:ti"), o id da variante da lista ou "auto". */
data class QualityOption(val id: String, val label: String, val selected: Boolean, val kind: QualityKind)

/** Faixa de vídeo de um stream adaptativo. */
data class VideoTrack(val id: String, val height: Int, val bitrate: Int)

/** Variante do mesmo título/canal na lista (4K/FHD/HD/SD). */
data class SourceVariant(val id: String, val label: String)

/** Monta a lista do menu Qualidade (puro, para testar). */
object QualityOptions {
    const val AUTO_ID = "auto"

    /**
     * @param tracks faixas de vídeo do stream (só vale quando há mais de uma altura distinta).
     * @param manualTrackId faixa escolhida à mão, ou null (Auto).
     * @param sources variantes da lista (só vale quando há mais de uma).
     * @param playingSourceId variante em reprodução.
     */
    fun build(
        tracks: List<VideoTrack>,
        manualTrackId: String?,
        sources: List<SourceVariant>,
        playingSourceId: String?,
    ): List<QualityOption> = buildList {
        val byHeight = tracks.filter { it.height > 0 }
            .groupBy { it.height }
            .map { (_, same) -> same.maxBy { it.bitrate } }
            .sortedByDescending { it.height }
        if (byHeight.size > 1) {
            add(QualityOption(AUTO_ID, "Auto", manualTrackId == null, QualityKind.AUTO))
            byHeight.forEach { add(QualityOption(it.id, "${it.height}p", it.id == manualTrackId, QualityKind.TRACK)) }
        }
        if (sources.size > 1) {
            sources.forEach { add(QualityOption(it.id, it.label, it.id == playingSourceId, QualityKind.SOURCE)) }
        }
    }
}

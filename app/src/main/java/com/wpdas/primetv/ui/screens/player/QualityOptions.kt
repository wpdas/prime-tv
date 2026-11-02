package com.wpdas.primetv.ui.screens.player

enum class QualityKind { AUTO, TRACK, SOURCE }

/**
 * Opção do menu Qualidade: [id] é a faixa ("gi:ti"), o id da variante da lista ou "auto".
 * [labelKey] (i18n) vem preenchida quando a lista não informa a qualidade ("Padrão"); [label] então
 * é um complemento opcional, como a resolução real em reprodução.
 */
data class QualityOption(val id: String, val label: String, val selected: Boolean, val kind: QualityKind, val labelKey: String? = null)

/** Faixa de vídeo de um stream adaptativo. */
data class VideoTrack(val id: String, val height: Int, val bitrate: Int)

/** Variante do mesmo título/canal na lista. [label] null = nome sem tag de qualidade; [detail] = resolução conhecida ("1080p"). */
data class SourceVariant(val id: String, val label: String?, val detail: String? = null)

/** Monta a lista do menu Qualidade (puro, para testar). */
object QualityOptions {
    const val AUTO_ID = "auto"
    const val DEFAULT_LABEL_KEY = "player.quality_default"

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
            sources.forEach {
                val label = it.label ?: it.detail.orEmpty()
                add(QualityOption(it.id, label, it.id == playingSourceId, QualityKind.SOURCE, labelKey = if (it.label == null) DEFAULT_LABEL_KEY else null))
            }
        }
    }
}

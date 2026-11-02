package com.wpdas.primetv.domain.model

/** Intervalo de atualização automática de uma seção. */
enum class SyncInterval(val hours: Int, val labelKey: String) {
    H6(6, "interval.h6"),
    H12(12, "interval.h12"),
    D1(24, "interval.d1"),
    D3(72, "interval.d3"),
    D7(168, "interval.d7"),
    MANUAL(0, "interval.manual");

    val millis: Long get() = hours * 60L * 60L * 1000L
    val isManual: Boolean get() = this == MANUAL

    fun next(): SyncInterval = entries[(ordinal + 1) % entries.size]

    companion object {
        /** Canais de evento ("Jogos do Dia") são renomeados ao longo do dia: Ao vivo e Guia a cada 6 h; filmes e séries por dia. */
        fun defaultFor(section: ContentSection) = when (section) {
            ContentSection.LIVE, ContentSection.EPG -> H6
            else -> D1
        }
    }
}

/** Tamanho do buffer do player. */
enum class BufferProfile(val labelKey: String, val minBufferMs: Int, val maxBufferMs: Int, val playbackMs: Int, val rebufferMs: Int) {
    LOW("buffer.low", 8_000, 30_000, 1_000, 3_000),
    NORMAL("buffer.normal", 20_000, 60_000, 1_500, 5_000),
    HIGH("buffer.high", 40_000, 120_000, 3_000, 10_000);

    fun next(): BufferProfile = entries[(ordinal + 1) % entries.size]
}

data class AppSettings(
    val syncIntervals: Map<ContentSection, SyncInterval> = ContentSection.entries.associateWith { SyncInterval.defaultFor(it) },
    val unmeteredOnly: Boolean = false,
    val footballKeywords: List<String> = emptyList(),
    val preferSoftwareDecoder: Boolean = false,
    val bufferProfile: BufferProfile = BufferProfile.NORMAL,
    val language: String = "pt-BR",
    /** Margem de segurança (overscan) em % de cada borda: TVs que cortam as bordas da imagem. */
    val overscanPercent: Int = 0,
    val previewSound: Boolean = false,
) {
    companion object {
        val OVERSCAN_OPTIONS = listOf(0, 2, 4, 6)
    }
    fun interval(section: ContentSection): SyncInterval = syncIntervals[section] ?: SyncInterval.defaultFor(section)
}

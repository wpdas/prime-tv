package com.wpdas.primetv.domain.model

data class EpgProgram(
    val channelKey: String,
    val start: Long,
    val end: Long,
    val title: String,
    val description: String?,
) {
    fun progressAt(now: Long): Float =
        if (end <= start) 0f else ((now - start).toFloat() / (end - start)).coerceIn(0f, 1f)
}

data class NowNext(val now: EpgProgram?, val next: List<EpgProgram>)

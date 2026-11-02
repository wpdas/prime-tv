package com.wpdas.primetv.domain.football

import com.wpdas.primetv.core.util.Quality
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.domain.model.Channel
import com.wpdas.primetv.domain.model.EpgProgram
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

enum class MatchStatus { UPCOMING, LIVE, FINISHED }

/** Um jogo anunciado na lista, com o canal que o transmite. */
data class MatchEvent(
    val key: String,
    val home: String,
    val away: String,
    /** Início em epoch ms. */
    val start: Long,
    /** O que a lista diz da competição ("Série B", "Brasileirão"); null quando não diz. */
    val leagueHint: String?,
    val channelId: String,
    val channelName: String,
    val categoryName: String?,
    /** Outros canais que transmitem o mesmo jogo. */
    val altChannelIds: List<String> = emptyList(),
    /** true quando o horário veio do nome do canal (slot "Jogos do Dia"). */
    val fromChannelName: Boolean,
) {
    fun status(now: Long): MatchStatus = when {
        now < start -> MatchStatus.UPCOMING
        now < start + LIVE_WINDOW_MS -> MatchStatus.LIVE
        else -> MatchStatus.FINISHED
    }

    companion object {
        /** Um jogo dura ~2h15 com intervalo e acréscimos. */
        const val LIVE_WINDOW_MS = 135 * 60_000L
    }
}

/**
 * Monta o guia de jogos a partir do que a lista traz:
 *  1. canais de evento cujo nome tem horário: "Juventus x Napoli - 16:45 FHD" (o EPG do canal dá a data);
 *  2. EPG dos canais de futebol reconhecidos por palavra-chave (Premiere etc.) com título "time x time".
 * Slots genéricos ("Brasileirão", "Jogos do Dia") nunca entram. Sem chute de liga: só o que a lista diz.
 */
object MatchSchedule {

    data class NamedMatch(val home: String, val away: String, val hour: Int, val minute: Int, val leagueHint: String?)

    private val NAME_WITH_TIME = Regex(
        """^(.+?)\s+(?:x|vs\.?|×)\s+(.+?)\s*[-–]\s*(\d{1,2})[:h](\d{2})(?:\s+(.*))?$""",
        RegexOption.IGNORE_CASE,
    )
    /** Esportes que também aparecem como "time x time" mas não são futebol. */
    private val OTHER_SPORTS = Regex("""(?i)(?<![a-z])(nfl|nba|nhl|mlb|ufc|mma|volei|vôlei|basquete|tenis|tênis|f1|formula|fórmula|wwe)(?![a-z])""")
    /** Reprises e anúncios de jogos futuros no EPG ("VT - A x B", "Dia 04/11 - A x B"): não são o jogo em si. */
    private val NOT_LIVE = Regex("""(?i)(?<![a-z])(vt|reprise|replay|compacto|melhores momentos|dia\s+\d{1,2}/\d{1,2})(?![a-z])""")
    /** Palavras depois do horário que não são competição. */
    private val TAIL_NOISE = Regex("""(?i)(?<![a-z])(ao vivo|jogo do dia|vt|reprise|replay|compacto)(?![a-z])""")
    private const val MERGE_WINDOW_MS = 3 * 60 * 60_000L

    /** "Cuiabá x São Bernardo - 18:30 Série B FHD" -> times, 18:30, "Série B". Null se não houver horário. */
    fun parseName(name: String): NamedMatch? {
        val cleaned = Quality.stripTags(name)
        val m = NAME_WITH_TIME.find(cleaned) ?: return null
        val home = m.groupValues[1].trim()
        val away = m.groupValues[2].trim()
        val hour = m.groupValues[3].toIntOrNull() ?: return null
        val minute = m.groupValues[4].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        if (home.length < 2 || away.length < 2 || home.length > 40 || away.length > 40) return null
        val tail = m.groupValues[5].replace(TAIL_NOISE, " ").replace(Regex("\\s+"), " ").trim().trim('-', '–', '|', ':')
        return NamedMatch(home, away, hour, minute, tail.ifBlank { null })
    }

    /** Slot genérico do provedor: nome igual à categoria ("Brasileirão" em BRASILEIRAO, "Jogos do Dia"). */
    fun isPlaceholder(channel: Channel): Boolean {
        val n = TextNormalizer.searchKey(Quality.stripTags(channel.name))
        val c = channel.categoryName?.let { TextNormalizer.searchKey(it) }.orEmpty()
        return n.isNotEmpty() && (n == c || n == "jogos do dia" || n == "jogo do dia")
    }

    /**
     * @param channels canais de futebol já sem variantes de qualidade (a melhor de cada grupo).
     * @param programs programas do EPG desses canais no período de interesse.
     * @param keywordMatch canais em que a classificação veio de palavra-chave (não só do "x"); só o EPG deles vale como fonte 2.
     */
    fun build(
        channels: List<Channel>,
        programs: List<EpgProgram>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        keywordMatch: (Channel) -> Boolean = { false },
    ): List<MatchEvent> {
        val byKey = programs.groupBy { it.channelKey }
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val events = ArrayList<MatchEvent>()

        // Fonte 1: horário no nome do canal.
        val slotChannels = HashSet<String>()
        for (ch in channels) {
            if (isPlaceholder(ch)) continue
            val named = parseName(ch.name) ?: continue
            slotChannels += ch.id
            val start = resolveStart(named, ch.epgKey?.let { byKey[it] }.orEmpty(), today, zone)
            val hint = named.leagueHint ?: categoryHint(ch.categoryName)
            merge(events, MatchEvent(
                key = eventKey(named.home, named.away, start, zone),
                home = named.home, away = named.away, start = start, leagueHint = hint,
                channelId = ch.id, channelName = ch.displayName, categoryName = ch.categoryName,
                fromChannelName = true,
            ))
        }

        // Fonte 2: EPG dos canais de futebol por palavra-chave.
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        for (ch in channels) {
            if (ch.id in slotChannels || isPlaceholder(ch) || !keywordMatch(ch)) continue
            val key = ch.epgKey ?: continue
            for (p in byKey[key].orEmpty()) {
                if (p.start < dayStart || p.start >= dayEnd) continue
                if (OTHER_SPORTS.containsMatchIn(p.title) || NOT_LIVE.containsMatchIn(p.title)) continue
                val match = MatchTitle.parse(p.title) ?: continue
                merge(events, MatchEvent(
                    key = eventKey(match.home, match.away, p.start, zone),
                    home = match.home, away = match.away, start = p.start,
                    leagueHint = titleHint(p.title, match) ?: categoryHint(ch.categoryName),
                    channelId = ch.id, channelName = ch.displayName, categoryName = ch.categoryName,
                    fromChannelName = false,
                ))
            }
        }

        // Jogos de dias anteriores não interessam mais (os slots de ontem podem continuar na lista até a próxima atualização).
        val dayStartMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val current = events.filter { it.start >= dayStartMs }
        // Mesmos times duas vezes no dia (raro): chaves ainda assim únicas para a lista.
        val seen = HashMap<String, Int>()
        val unique = current.map { e ->
            val n = seen.merge(e.key, 1, Int::plus) ?: 1
            if (n == 1) e else e.copy(key = "${e.key}#$n")
        }
        return unique.sortedWith(
            compareBy<MatchEvent> { if (it.status(now) == MatchStatus.FINISHED) 1 else 0 }
                .thenBy { if (it.status(now) == MatchStatus.LIVE) 0 else 1 }
                .thenBy { it.start }
                .thenBy { it.home },
        )
    }

    /** Data do jogo: programa do EPG com o mesmo horário (hoje, amanhã ou ontem, nessa ordem); senão hoje. */
    private fun resolveStart(named: NamedMatch, programs: List<EpgProgram>, today: LocalDate, zone: ZoneId): Long {
        val time = LocalTime.of(named.hour, named.minute)
        val candidates = programs.filter {
            val local = Instant.ofEpochMilli(it.start).atZone(zone)
            local.toLocalTime() == time
        }
        val preferred = listOf(today, today.plusDays(1), today.minusDays(1))
        for (day in preferred) {
            candidates.firstOrNull { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() == day }?.let { return it.start }
        }
        return today.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    /** Categoria da lista que já nomeia a competição. */
    private fun categoryHint(category: String?): String? {
        val c = category?.let { TextNormalizer.searchKey(it) } ?: return null
        return when {
            c.contains("brasileir") -> "Brasileirão"
            c.contains("estadua") -> "Estaduais"
            c.contains("libertadores") -> "Libertadores"
            c.contains("copa do brasil") -> "Copa do Brasil"
            c.contains("champions") -> "Champions League"
            else -> null
        }
    }

    /** "Palmeiras x Vasco - Copa do Brasil ║ ..." -> "Copa do Brasil"; "… | LALIGA 26/27 | 11ª RODADA" -> "LALIGA 26/27". */
    private fun titleHint(title: String, match: Match): String? {
        val idx = title.indexOf(match.away, ignoreCase = true).takeIf { it >= 0 } ?: return null
        val rest = title.substring(idx + match.away.length)
        val m = Regex("""^\s*[-–|║:]\s*([^-–|║:]+)""").find(rest) ?: return null
        val hint = m.groupValues[1].replace(TAIL_NOISE, " ").replace(Regex("\\s+"), " ").trim()
        if (hint.length !in 3..30 || MatchTitle.parse(hint) != null) return null
        return hint
    }

    private fun eventKey(home: String, away: String, start: Long, zone: ZoneId): String =
        "${TeamNames.key(home)}|${TeamNames.key(away)}|${Instant.ofEpochMilli(start).atZone(zone).toLocalDate()}"

    /** Mesmo jogo (times iguais, mesmo dia, horário próximo) vira um só evento; o slot nomeado é o principal. */
    private fun merge(events: MutableList<MatchEvent>, event: MatchEvent) {
        val index = events.indexOfFirst {
            TeamNames.same(it.home, event.home) && TeamNames.same(it.away, event.away) && abs(it.start - event.start) <= MERGE_WINDOW_MS
        }
        if (index < 0) {
            events += event
            return
        }
        val existing = events[index]
        val primary = if (!existing.fromChannelName && event.fromChannelName) event else existing
        val secondary = if (primary === event) existing else event
        events[index] = primary.copy(
            leagueHint = primary.leagueHint ?: secondary.leagueHint,
            altChannelIds = (primary.altChannelIds + secondary.channelId + secondary.altChannelIds).distinct().filter { it != primary.channelId },
        )
    }
}

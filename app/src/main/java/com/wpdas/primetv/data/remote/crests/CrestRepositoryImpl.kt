package com.wpdas.primetv.data.remote.crests

import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.LeagueBadgeEntity
import com.wpdas.primetv.data.local.db.MatchLeagueEntity
import com.wpdas.primetv.domain.crests.LeagueInfo
import com.wpdas.primetv.data.local.db.TeamCrestEntity
import com.wpdas.primetv.domain.crests.TeamCrestRepository
import com.wpdas.primetv.domain.crests.TeamInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Escudos e ligas com cache: memória -> Room (30 dias; 7 dias para "não achei") -> rede.
 * Uma chamada por vez, com intervalo mínimo, porque a chave de teste da base é compartilhada e
 * limitada; falha de rede ou limite (429) não vira cache negativo — tenta de novo mais tarde.
 */
@Singleton
class CrestRepositoryImpl @Inject constructor(
    private val db: PrimeDatabase,
    http: OkHttpClient,
    json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TeamCrestRepository {

    private val api: SportsDbApi = Retrofit.Builder()
        .baseUrl(SportsDbApi.BASE_URL)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SportsDbApi::class.java)

    private val teams = ConcurrentHashMap<String, TeamInfo>()
    private val leagues = ConcurrentHashMap<String, String?>()
    private val matches = ConcurrentHashMap<String, java.util.Optional<LeagueInfo>>()
    private val lock = Mutex()
    /** Próximo instante em que a rede pode ser chamada (espaçamento e recuo após 429). */
    private var nextCallAt = 0L

    override suspend fun teamInfo(team: String): TeamInfo {
        val key = CrestNames.key(team)
        if (key.isBlank()) return NONE
        teams[key]?.let { return it }
        return withContext(io) {
            lock.withLock {
                teams[key]?.let { return@withLock it }
                val dao = db.tmdbDao()
                val cached = dao.crest(key)
                val now = System.currentTimeMillis()
                if (cached != null && now - cached.fetchedAt < if (cached.url != null) POSITIVE_TTL_MS else NEGATIVE_TTL_MS) {
                    return@withLock TeamInfo(cached.url, cached.league, cached.leagueId, cached.name).also { teams[key] = it }
                }
                val fetched = runCatching { throttled { fetchTeam(team) } }.getOrNull()
                if (fetched == null) {
                    // Sem resposta: fica o que havia (ou nada), sem gravar "não achei".
                    return@withLock cached?.let { TeamInfo(it.url, it.league, it.leagueId, it.name) } ?: NONE
                }
                dao.putCrest(TeamCrestEntity(key, fetched.crestUrl, now, fetched.league, fetched.leagueId, fetched.name))
                teams[key] = fetched
                fetched
            }
        }
    }

    override suspend fun leagueBadge(leagueId: String): String? {
        if (leagueId.isBlank()) return null
        if (leagues.containsKey(leagueId)) return leagues[leagueId]
        return withContext(io) {
            lock.withLock {
                if (leagues.containsKey(leagueId)) return@withLock leagues[leagueId]
                val dao = db.tmdbDao()
                val cached = dao.leagueBadge(leagueId)
                val now = System.currentTimeMillis()
                if (cached != null && now - cached.fetchedAt < if (cached.url != null) POSITIVE_TTL_MS else NEGATIVE_TTL_MS) {
                    leagues[leagueId] = cached.url
                    return@withLock cached.url
                }
                val result = runCatching { throttled { fetchLeague(leagueId) } }
                if (result.isFailure) return@withLock cached?.url
                val url = result.getOrNull()
                dao.putLeagueBadge(LeagueBadgeEntity(leagueId, url, now))
                leagues[leagueId] = url
                url
            }
        }
    }

    override suspend fun matchLeague(home: String, away: String, startMs: Long): LeagueInfo? {
        val day = MatchEvents.dayOf(startMs)
        val key = MatchEvents.key(home, away, day)
        if (key.isBlank()) return null
        matches[key]?.let { return it.orElse(null) }
        return withContext(io) {
            // Nomes como a base escreve (mesma consulta do escudo, já em cache na maioria das vezes).
            val homeName = teamInfo(home).name ?: home
            val awayName = teamInfo(away).name ?: away
            lock.withLock {
                matches[key]?.let { return@withLock it.orElse(null) }
                val dao = db.tmdbDao()
                val cached = dao.matchLeague(key)
                val now = System.currentTimeMillis()
                if (cached != null && now - cached.fetchedAt < if (cached.league != null) MATCH_TTL_MS else MATCH_NEGATIVE_TTL_MS) {
                    val info = cached.league?.let { LeagueInfo(it, cached.leagueId, cached.badgeUrl) }
                    matches[key] = java.util.Optional.ofNullable(info)
                    return@withLock info
                }
                val result = runCatching { throttled { fetchMatch(homeName, awayName, day) } }
                if (result.isFailure) return@withLock cached?.league?.let { LeagueInfo(it, cached.leagueId, cached.badgeUrl) }
                val info = result.getOrNull()
                dao.putMatchLeague(MatchLeagueEntity(key, info?.league, info?.leagueId, info?.badgeUrl, now))
                matches[key] = java.util.Optional.ofNullable(info)
                info
            }
        }
    }

    private suspend fun fetchMatch(homeName: String, awayName: String, day: java.time.LocalDate): LeagueInfo? {
        val found = api.searchEvents(MatchEvents.query(homeName, awayName)).events.orEmpty()
        val event = MatchEvents.pick(homeName, awayName, day, found) ?: return null
        val league = event.league?.takeIf { it.isNotBlank() } ?: return null
        val badge = event.leagueBadge?.takeIf { it.isNotBlank() }?.let { "$it/small" }
        return LeagueInfo(league, event.leagueId?.takeIf { it.isNotBlank() }, badge)
    }

    private suspend fun fetchTeam(team: String): TeamInfo {
        val query = CrestNames.searchName(team)
        val found = api.searchTeams(query).teams.orEmpty()
        val pick = CrestNames.pick(query, found) ?: return NONE
        return TeamInfo(pick.badge?.let { "$it/small" }, pick.league?.takeIf { it.isNotBlank() }, pick.leagueId?.takeIf { it.isNotBlank() }, pick.name?.takeIf { it.isNotBlank() })
    }

    private suspend fun fetchLeague(id: String): String? {
        val league = api.lookupLeague(id).leagues.orEmpty().firstOrNull() ?: return null
        return (league.badge ?: league.logo)?.takeIf { it.isNotBlank() }?.let { "$it/small" }
    }

    /**
     * Espaça as chamadas (a chave de teste aceita ~30 por minuto) e, ao receber 429, espera e tenta de novo
     * algumas vezes antes de desistir. Chamado sempre dentro de [lock].
     */
    private suspend fun <T> throttled(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            val wait = nextCallAt - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            nextCallAt = System.currentTimeMillis() + MIN_INTERVAL_MS
            try {
                return block()
            } catch (e: retrofit2.HttpException) {
                if (e.code() != 429 || ++attempt >= MAX_ATTEMPTS) throw e
                nextCallAt = System.currentTimeMillis() + RATE_LIMIT_BACKOFF_MS
                if (com.wpdas.primetv.BuildConfig.DEBUG) android.util.Log.w(TAG, "Limite da base de escudos atingido; nova tentativa em ${RATE_LIMIT_BACKOFF_MS / 1000} s")
            }
        }
    }

    private companion object {
        val NONE = TeamInfo(null, null)
        const val POSITIVE_TTL_MS = 30L * 24 * 60 * 60 * 1000
        const val NEGATIVE_TTL_MS = 7L * 24 * 60 * 60 * 1000
        /** Partida: vale pelo dia do jogo; "não achei" tenta de novo em algumas horas (a base pode cadastrar depois). */
        const val MATCH_TTL_MS = 3L * 24 * 60 * 60 * 1000
        const val MATCH_NEGATIVE_TTL_MS = 6L * 60 * 60 * 1000
        const val TAG = "Crests"
        const val MIN_INTERVAL_MS = 2_100L
        const val RATE_LIMIT_BACKOFF_MS = 45_000L
        const val MAX_ATTEMPTS = 3
    }
}

/** Busca de partida na base pública: chave de cache, texto da consulta e escolha do evento certo. */
object MatchEvents {
    private val zone: java.time.ZoneId get() = java.time.ZoneId.systemDefault()

    fun dayOf(startMs: Long): java.time.LocalDate = java.time.Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate()

    fun key(home: String, away: String, day: java.time.LocalDate): String {
        val h = com.wpdas.primetv.domain.football.TeamNames.key(home)
        val a = com.wpdas.primetv.domain.football.TeamNames.key(away)
        return if (h.isEmpty() || a.isEmpty()) "" else "$h|$a|$day"
    }

    /** "Manchester United" + "Roma" -> "Manchester_United_vs_Roma" (a base aceita sem acentos). */
    fun query(home: String, away: String): String =
        "${TextNormalizer.stripAccents(home).trim()}_vs_${TextNormalizer.stripAccents(away).trim()}".replace(' ', '_')

    /**
     * Só futebol, times iguais aos pedidos (na mesma ordem) e data no dia do jogo ou vizinho (fuso da base
     * pode diferir); empate, fica a data exata.
     */
    fun pick(home: String, away: String, day: java.time.LocalDate, events: List<SportsDbEvent>): SportsDbEvent? {
        return events
            .filter { it.sport.equals("Soccer", ignoreCase = true) }
            .filter { same(home, it.home.orEmpty()) && same(away, it.away.orEmpty()) }
            .mapNotNull { e -> runCatching { java.time.LocalDate.parse(e.date) }.getOrNull()?.let { it to e } }
            .filter { (d, _) -> kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(day, d)) <= 1 }
            .minByOrNull { (d, _) -> kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(day, d)) }
            ?.second
    }

    /** [TeamNames.same] mais um prefixo/sufixo de clube ("VfB Stuttgart" ~ "Stuttgart"), com a parte comum de pelo menos 4 letras. */
    private fun same(a: String, b: String): Boolean {
        if (com.wpdas.primetv.domain.football.TeamNames.same(a, b)) return true
        val ka = com.wpdas.primetv.domain.football.TeamNames.key(a)
        val kb = com.wpdas.primetv.domain.football.TeamNames.key(b)
        val (short, long) = if (ka.length <= kb.length) ka to kb else kb to ka
        return short.length >= 4 && (long.endsWith(" $short") || long.startsWith("$short "))
    }
}

/** Nomes como aparecem nas listas -> nome que a base conhece; e escolha do resultado certo. */
object CrestNames {
    private val ALIASES = mapOf(
        "atletico mg" to "Atletico Mineiro", "atletico mineiro" to "Atletico Mineiro", "galo" to "Atletico Mineiro", "cam" to "Atletico Mineiro",
        "atletico pr" to "Athletico Paranaense", "athletico pr" to "Athletico Paranaense", "athletico" to "Athletico Paranaense",
        "atletico go" to "Atletico Goianiense", "america mg" to "America Mineiro", "america mineiro" to "America Mineiro",
        "inter" to "Internacional", "sc internacional" to "Internacional", "sao paulo" to "Sao Paulo", "spfc" to "Sao Paulo",
        "fla" to "Flamengo", "flu" to "Fluminense", "bota" to "Botafogo", "vasco" to "Vasco da Gama", "vasco da gama" to "Vasco da Gama",
        "red bull bragantino" to "Bragantino", "rb bragantino" to "Bragantino", "man city" to "Manchester City", "man united" to "Manchester United",
        "man utd" to "Manchester United", "psg" to "Paris Saint-Germain", "barca" to "Barcelona", "bayern" to "Bayern Munich",
        "bayern de munique" to "Bayern Munich", "inter de milao" to "Inter Milan", "internazionale" to "Inter Milan",
    )
    fun key(team: String): String = com.wpdas.primetv.domain.football.TeamNames.key(team)

    fun searchName(team: String): String {
        val k = key(team)
        return ALIASES[k] ?: TextNormalizer.stripAccents(team).replace(Regex("""\s*[-–]\s*[A-Z]{2}$"""), "").trim()
    }

    /** Só futebol; prefere Brasil e exige que o nome lembre o buscado (evita "Intercity" para "Inter"). */
    fun pick(query: String, teams: List<SportsDbTeam>): SportsDbTeam? {
        val wanted = key(query)
        val soccer = teams.filter { it.sport.equals("Soccer", ignoreCase = true) && !it.badge.isNullOrBlank() }
        fun matches(t: SportsDbTeam): Boolean {
            val names = listOfNotNull(t.name, t.alternate).flatMap { it.split(',') }.map { key(it) }
            return names.any { it == wanted || it.startsWith("$wanted ") || wanted.startsWith("$it ") || it.endsWith(" $wanted") }
        }
        fun exact(t: SportsDbTeam) = listOfNotNull(t.name, t.alternate).flatMap { it.split(',') }.any { key(it) == wanted }
        val candidates = soccer.filter(::matches)
        // Nome exato primeiro ("Botafogo" antes de "Botafogo SP"), depois o time principal (não o feminino, salvo se
        // o nome pedir), depois Brasil, depois o resto.
        val wantsWomen = Regex("""(?i)\b(women|feminino|femenino|ladies|fem)\b""").containsMatchIn(query)
        return candidates.sortedWith(
            compareByDescending<SportsDbTeam> { exact(it) }
                .thenByDescending { wantsWomen == it.gender.equals("Female", ignoreCase = true) }
                .thenByDescending { it.country.equals("Brazil", ignoreCase = true) },
        ).firstOrNull()
    }
}

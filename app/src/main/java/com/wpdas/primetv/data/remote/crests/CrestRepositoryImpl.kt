package com.wpdas.primetv.data.remote.crests

import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.LeagueBadgeEntity
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
                    return@withLock TeamInfo(cached.url, cached.league, cached.leagueId).also { teams[key] = it }
                }
                val fetched = runCatching { throttled { fetchTeam(team) } }.getOrNull()
                if (fetched == null) {
                    // Sem resposta: fica o que havia (ou nada), sem gravar "não achei".
                    return@withLock cached?.let { TeamInfo(it.url, it.league, it.leagueId) } ?: NONE
                }
                dao.putCrest(TeamCrestEntity(key, fetched.crestUrl, now, fetched.league, fetched.leagueId))
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

    private suspend fun fetchTeam(team: String): TeamInfo {
        val query = CrestNames.searchName(team)
        val found = api.searchTeams(query).teams.orEmpty()
        val pick = CrestNames.pick(query, found) ?: return NONE
        return TeamInfo(pick.badge?.let { "$it/small" }, pick.league?.takeIf { it.isNotBlank() }, pick.leagueId?.takeIf { it.isNotBlank() })
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
        const val TAG = "Crests"
        const val MIN_INTERVAL_MS = 2_100L
        const val RATE_LIMIT_BACKOFF_MS = 45_000L
        const val MAX_ATTEMPTS = 3
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

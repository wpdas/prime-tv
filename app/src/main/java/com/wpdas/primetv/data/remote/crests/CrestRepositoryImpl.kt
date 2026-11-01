package com.wpdas.primetv.data.remote.crests

import com.wpdas.primetv.core.di.IoDispatcher
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.data.local.db.PrimeDatabase
import com.wpdas.primetv.data.local.db.TeamCrestEntity
import com.wpdas.primetv.domain.crests.TeamCrestRepository
import kotlinx.coroutines.CoroutineDispatcher
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
 * Escudos reais com cache: memória -> Room (30 dias; 7 dias para "não achei") -> rede.
 * Uma busca por time de cada vez, para não martelar a API de teste.
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

    private val memory = ConcurrentHashMap<String, String?>()
    private val lock = Mutex()

    override suspend fun crestUrl(team: String): String? {
        val key = CrestNames.key(team)
        if (key.isBlank()) return null
        if (memory.containsKey(key)) return memory[key]
        return withContext(io) {
            lock.withLock {
                if (memory.containsKey(key)) return@withLock memory[key]
                val dao = db.tmdbDao()
                val cached = dao.crest(key)
                val now = System.currentTimeMillis()
                if (cached != null && now - cached.fetchedAt < if (cached.url != null) POSITIVE_TTL_MS else NEGATIVE_TTL_MS) {
                    memory[key] = cached.url
                    return@withLock cached.url
                }
                val url = runCatching { fetch(team) }.getOrNull() ?: cached?.url
                dao.putCrest(TeamCrestEntity(key, url, now))
                memory[key] = url
                url
            }
        }
    }

    private suspend fun fetch(team: String): String? {
        val query = CrestNames.searchName(team)
        val teams = api.searchTeams(query).teams.orEmpty()
        return CrestNames.pick(query, teams)?.badge?.let { "$it/small" }
    }

    private companion object {
        const val POSITIVE_TTL_MS = 30L * 24 * 60 * 60 * 1000
        const val NEGATIVE_TTL_MS = 7L * 24 * 60 * 60 * 1000
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
    private val SUFFIXES = Regex("""\b(fc|ec|sc|ac|clube|club|futebol|esporte|sport|saf)\b""")

    fun key(team: String): String = TextNormalizer.searchKey(team).replace(SUFFIXES, " ").replace(Regex("\\s+"), " ").trim()

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
        // Nome exato primeiro ("Botafogo" antes de "Botafogo SP"), depois Brasil, depois o resto.
        return candidates.sortedWith(compareByDescending<SportsDbTeam> { exact(it) }.thenByDescending { it.country.equals("Brazil", ignoreCase = true) }).firstOrNull()
    }
}

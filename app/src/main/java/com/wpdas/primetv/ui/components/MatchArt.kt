package com.wpdas.primetv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.wpdas.primetv.core.util.TextNormalizer
import com.wpdas.primetv.ui.theme.InterFontFamily
import com.wpdas.primetv.ui.theme.PrimeTheme

/** Partida reconhecida num nome: "Flamengo x Palmeiras". */
data class Match(val home: String, val away: String)

/**
 * Arte gerada para jogos sem imagem: escudo do time A, "×" e escudo do time B.
 * Sem imagens de clubes (marca registrada): cada escudo usa as cores conhecidas do clube
 * ou cores derivadas do nome, com a sigla por cima.
 */
object MatchArtist {
    private val VERSUS = Regex("""^(.*?[\p{L}\d].*?)\s+(?:x|vs\.?|×|v)\s+(.*?[\p{L}\d].*?)$""", RegexOption.IGNORE_CASE)
    /** Prefixos que não são time: "VT - ", "AO VIVO: ", "Dia 04/11 - ", "18:30 - ", "Jogo 2 - ". */
    private val NOISE = Regex("""(?i)^(?:(?:vt|ao vivo|reprise|replay|compacto|melhores momentos|jogo\s*\d*|dia\s+\d{1,2}/\d{1,2}|\d{1,2}(?::|h)\d{2})\s*[-–:|]?\s*)+""")
    /** Corta o que vem depois de " - ", " | " ou " (": "Atlético-MG (Premiere)" -> "Atlético-MG". */
    private val TRAILING = Regex("""\s+[-–|]\s.*$|\s*\(.*$""")

    /** Extrai os dois times de um título; null quando não é uma partida. */
    fun parse(title: String): Match? {
        val cleaned = title.replace(NOISE, "").trim()
        val m = VERSUS.find(cleaned) ?: return null
        val home = m.groupValues[1].replace(TRAILING, "").trim().trimEnd('-', '–', ':')
        val away = m.groupValues[2].replace(TRAILING, "").trim()
        if (home.length < 2 || away.length < 2 || home.length > 40 || away.length > 40) return null
        return Match(home, away)
    }

    /** "Atlético-MG" -> "CAM"? Simples e previsível: iniciais das palavras ou 3 primeiras letras. */
    fun abbreviation(team: String): String {
        val key = TextNormalizer.stripAccents(team).uppercase()
        KNOWN[normalizedKey(team)]?.let { return it.abbreviation }
        val words = key.split(' ', '-').filter { it.isNotBlank() && it.length > 1 && it !in STOP }
        return when {
            words.size >= 3 -> words.take(3).joinToString("") { it.first().toString() }
            words.size == 2 -> (words[0].take(2) + words[1].take(1))
            else -> key.filter { it.isLetterOrDigit() }.take(3)
        }.take(3)
    }

    fun colors(team: String): Pair<Color, Color> {
        KNOWN[normalizedKey(team)]?.let { return it.primary to it.secondary }
        val h = (team.hashCode() and 0x7fffffff)
        val palette = listOf(
            Color(0xFFC0392B) to Color(0xFF1B1B1B), Color(0xFF1E6F3F) to Color(0xFFF2F2F2), Color(0xFF1F4E9A) to Color(0xFFF2F2F2),
            Color(0xFF8E44AD) to Color(0xFFF2F2F2), Color(0xFFD35400) to Color(0xFF1B1B1B), Color(0xFF2C3E50) to Color(0xFFE67E22),
            Color(0xFF7B1E1E) to Color(0xFFF2F2F2), Color(0xFF0E7C7B) to Color(0xFFF2F2F2),
        )
        return palette[h % palette.size]
    }

    private fun normalizedKey(team: String) = TextNormalizer.stripAccents(team.lowercase()).replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    private data class Club(val abbreviation: String, val primary: Color, val secondary: Color)

    private val STOP = setOf("DE", "DO", "DA", "FC", "EC", "SC", "AC", "CLUBE", "CLUB", "SPORT", "ATLETICO", "ATHLETIC")

    private val KNOWN: Map<String, Club> = buildMap {
        fun put(abbr: String, primary: Long, secondary: Long, vararg names: String) {
            val club = Club(abbr, Color(primary), Color(secondary))
            names.forEach { put(it, club) }
        }
        put("FLA", 0xFFC8102E, 0xFF000000, "flamengo", "fla", "cr flamengo")
        put("PAL", 0xFF006437, 0xFFFFFFFF, "palmeiras", "pal", "se palmeiras")
        put("COR", 0xFF000000, 0xFFFFFFFF, "corinthians", "cor", "sc corinthians")
        put("SAO", 0xFFFE0000, 0xFF000000, "sao paulo", "spfc", "sao paulo fc", "sao")
        put("SAN", 0xFF000000, 0xFFFFFFFF, "santos", "santos fc", "san")
        put("GRE", 0xFF0D80BF, 0xFF000000, "gremio", "gre", "gremio fbpa")
        put("INT", 0xFFE5050F, 0xFFFFFFFF, "internacional", "inter", "int", "sc internacional")
        put("CAM", 0xFF000000, 0xFFFFFFFF, "atletico mg", "atletico mineiro", "galo", "cam", "atletico")
        put("CRU", 0xFF003399, 0xFFFFFFFF, "cruzeiro", "cru")
        put("BOT", 0xFF000000, 0xFFFFFFFF, "botafogo", "bot")
        put("VAS", 0xFF000000, 0xFFFFFFFF, "vasco", "vasco da gama", "vas")
        put("FLU", 0xFF7A1F3D, 0xFF006B3F, "fluminense", "flu")
        put("BAH", 0xFF0057A8, 0xFFE4002B, "bahia", "ec bahia", "bah")
        put("VIT", 0xFFE4002B, 0xFF000000, "vitoria", "ec vitoria", "vit")
        put("FOR", 0xFF0033A0, 0xFFE4002B, "fortaleza", "for")
        put("CEA", 0xFF000000, 0xFFFFFFFF, "ceara", "cea")
        put("CAP", 0xFFE4002B, 0xFF000000, "athletico pr", "athletico paranaense", "atletico pr", "cap", "athletico")
        put("COR", 0xFF000000, 0xFFFFFFFF, "coritiba", "coxa")
        put("GOI", 0xFF006B3F, 0xFFFFFFFF, "goias", "goi")
        put("CUI", 0xFF006B3F, 0xFFFFD700, "cuiaba", "cui")
        put("BGT", 0xFFE4002B, 0xFFFFFFFF, "bragantino", "red bull bragantino", "rb bragantino", "bgt")
        put("JUV", 0xFF006B3F, 0xFFFFFFFF, "juventude", "juv")
        put("CRI", 0xFFFFD700, 0xFF000000, "criciuma", "cri")
        put("AMG", 0xFF006B3F, 0xFF000000, "america mg", "america mineiro", "amg")
        put("SPT", 0xFFE4002B, 0xFF000000, "sport", "sport recife", "spt")
        put("LON", 0xFF003399, 0xFFFFFFFF, "londrina", "lon")
        put("MIR", 0xFFFFD700, 0xFF006B3F, "mirassol", "mir")
        put("RMA", 0xFFFFFFFF, 0xFF1A1A1A, "real madrid", "real", "rma")
        put("BAR", 0xFFA50044, 0xFF004D98, "barcelona", "barca", "bar")
        put("LIV", 0xFFC8102E, 0xFFFFFFFF, "liverpool", "liv")
        put("ARS", 0xFFEF0107, 0xFFFFFFFF, "arsenal", "ars")
        put("MCI", 0xFF6CABDD, 0xFFFFFFFF, "manchester city", "man city", "mci")
        put("MUN", 0xFFDA291C, 0xFF000000, "manchester united", "man united", "man utd", "mun")
        put("CHE", 0xFF034694, 0xFFFFFFFF, "chelsea", "che")
        put("JUVE", 0xFF000000, 0xFFFFFFFF, "juventus")
        put("PSG", 0xFF004170, 0xFFDA291C, "psg", "paris saint germain", "paris")
        put("BAY", 0xFFDC052D, 0xFFFFFFFF, "bayern", "bayern de munique", "bayern munich")
    }
}

/** Resolve a URL do escudo real (base pública de terceiros); null enquanto carrega ou se não houver. */
val LocalCrestResolver = androidx.compose.runtime.staticCompositionLocalOf<com.wpdas.primetv.domain.crests.TeamCrestRepository?> { null }

@Composable
private fun rememberCrestUrl(team: String): String? {
    val resolver = LocalCrestResolver.current ?: return null
    val state = androidx.compose.runtime.produceState<String?>(initialValue = null, key1 = team) {
        value = runCatching { resolver.crestUrl(team) }.getOrNull()
    }
    return state.value
}

/**
 * Escudo do time: a imagem real quando a base de terceiros a tiver (carregada da internet,
 * não fornecida pelo app); senão, o escudo estilizado com sigla.
 */
@Composable
fun TeamBadge(team: String, size: Dp, modifier: Modifier = Modifier) {
    val crest = rememberCrestUrl(team)
    if (crest != null) {
        coil3.compose.AsyncImage(
            model = crest,
            contentDescription = team,
            modifier = modifier.size(size),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )
        return
    }
    StylizedBadge(team, size, modifier)
}

/** Escudo estilizado com sigla, tamanho [size]. */
@Composable
fun StylizedBadge(team: String, size: Dp, modifier: Modifier = Modifier) {
    val (primary, secondary) = MatchArtist.colors(team)
    val abbreviation = MatchArtist.abbreviation(team)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w * 0.98f, h * 0.12f)
                lineTo(w * 0.92f, h * 0.62f)
                quadraticTo(w * 0.85f, h * 0.9f, w * 0.5f, h)
                quadraticTo(w * 0.15f, h * 0.9f, w * 0.08f, h * 0.62f)
                lineTo(w * 0.02f, h * 0.12f)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.75f))))
            drawPath(path, secondary.copy(alpha = 0.9f), style = Stroke(width = w * 0.06f))
            // faixa diagonal
            drawRect(secondary.copy(alpha = 0.25f), topLeft = Offset(0f, h * 0.42f), size = Size(w, h * 0.16f))
        }
        Text(
            abbreviation,
            color = if (secondary == Color(0xFF000000) || secondary == Color(0xFF1A1A1A) || secondary == Color(0xFF1B1B1B)) Color.White else secondary,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.3f).sp,
        )
    }
}

/** Dois escudos com um "×" no meio, sobre o fundo do app. */
@Composable
fun MatchArt(match: Match, modifier: Modifier = Modifier, badgeSize: Dp = 64.dp) {
    val colors = PrimeTheme.colors
    Box(
        modifier.background(Brush.linearGradient(listOf(colors.accentSoftBackground, colors.background))),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(badgeSize / 4)) {
            TeamBadge(match.home, badgeSize)
            Text("×", color = colors.text, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = (badgeSize.value * 0.45f).sp)
            TeamBadge(match.away, badgeSize)
        }
    }
}

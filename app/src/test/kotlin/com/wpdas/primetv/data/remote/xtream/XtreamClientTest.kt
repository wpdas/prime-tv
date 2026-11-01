package com.wpdas.primetv.data.remote.xtream

import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.PlaylistSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class XtreamClientTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }
    private lateinit var client: XtreamClient
    private lateinit var src: PlaylistSource.Xtream

    private fun fixture(name: String) = javaClass.classLoader!!.getResourceAsStream("xtream/$name")!!.bufferedReader().readText()

    @Before fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val url = request.requestUrl!!
                if (url.queryParameter("password") != "senha") return MockResponse().setResponseCode(200).setBody(fixture("auth_fail.json"))
                val body = when (url.queryParameter("action")) {
                    null -> fixture("auth_ok.json")
                    "get_live_categories" -> fixture("live_categories.json")
                    "get_live_streams" -> fixture("live_streams.json")
                    "get_vod_streams" -> fixture("vod_streams.json")
                    "get_series" -> fixture("series.json")
                    "get_series_info" -> fixture("series_info.json")
                    else -> "[]"
                }
                return MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body)
            }
        }
        server.start()
        client = XtreamClient(OkHttpClient(), json)
        val base = server.url("/").toString().trimEnd('/')
        src = PlaylistSource.Xtream(originalUrl = "$base/get.php", baseUrl = base, username = "usuario", password = "senha")
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `autentica e le user_info com tipos mistos`() {
        val auth = client.authenticate(src.baseUrl, "usuario", "senha")
        assertTrue(auth.userInfo!!.isAuthenticated)
        assertEquals("Active", auth.userInfo!!.status)
        assertEquals(listOf("m3u8", "ts", "rtmp"), auth.userInfo!!.allowedOutputFormats)
        assertEquals("8080", auth.serverInfo!!.port)
    }

    @Test fun `credenciais erradas voltam auth 0`() {
        val auth = client.authenticate(src.baseUrl, "usuario", "errada")
        assertEquals(false, auth.userInfo!!.isAuthenticated)
    }

    @Test fun `categorias com id nulo sao lidas sem quebrar`() {
        val cats = client.liveCategories(src)
        assertEquals(3, cats.size)
        assertEquals("195", cats[0].categoryId)
        assertNull(cats[2].categoryId)
    }

    @Test fun `streams ao vivo em streaming aceitam numero ou string em qualquer campo`() = runTest {
        val list = client.liveStreams(src) { it.toList() }
        assertEquals(4, list.size)
        assertEquals("28586", list[0].streamId)
        assertEquals("30001", list[1].streamId)
        assertEquals("2", list[1].num)
        assertNull(list[1].epgChannelId)
        assertEquals(true, list[2].isAdult.toBoolLenient())
        assertNull(list[3].streamId)
    }

    @Test fun `vod e series com rating e backdrop em formatos variados`() = runTest {
        val vod = client.vodStreams(src) { it.toList() }
        assertEquals(7.155, vod[0].rating.toDoubleLenient()!!, 0.001)
        assertEquals(0.0, vod[1].rating.toDoubleLenient()!!, 0.001)
        assertEquals(8899, vod[1].tmdbId.toIntLenient())
        val series = client.series(src) { it.toList() }
        assertEquals("4692", series[0].seriesId)
        assertEquals("4693", series[1].seriesId)
        assertEquals(2017, series[0].releaseDate.yearLenient())
        assertNull(series[1].releaseDate.yearLenient())
    }

    @Test fun `episodios agrupados por temporada viram lista unica`() {
        val info = client.seriesInfo(src, "4692")
        val eps = client.parseEpisodes(info)
        assertEquals(3, eps.size)
        assertEquals("255473", eps[0].id)
        assertEquals(2700, eps[0].info!!.durationSecs.toIntLenient())
        assertEquals("255474", eps[1].id)
        assertEquals(2, eps[1].episodeNum.toIntLenient())
        assertEquals("2", eps[2].season)
    }

    @Test fun `resposta que nao e json vira InvalidPlaylist`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(200).setBody("<html>oi</html>")
        }
        try {
            client.liveCategories(src)
            fail("deveria lançar")
        } catch (e: PlaylistException) {
            assertEquals(PlaylistError.InvalidPlaylist, e.error)
        }
    }

    @Test fun `http 403 vira InvalidCredentials e 500 vira ServerUnreachable`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(403)
        }
        try { client.liveCategories(src); fail() } catch (e: PlaylistException) { assertEquals(PlaylistError.InvalidCredentials, e.error) }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(500)
        }
        try { client.liveCategories(src); fail() } catch (e: PlaylistException) { assertEquals(PlaylistError.ServerUnreachable, e.error) }
    }

    @Test fun `monta urls de stream`() {
        assertEquals("${src.baseUrl}/live/usuario/senha/1.ts", XtreamClient.liveStreamUrl(src, "1"))
        assertEquals("${src.baseUrl}/movie/usuario/senha/2.mkv", XtreamClient.movieStreamUrl(src, "2", "mkv"))
        assertEquals("${src.baseUrl}/movie/usuario/senha/2.mp4", XtreamClient.movieStreamUrl(src, "2", null))
        assertEquals("${src.baseUrl}/series/usuario/senha/3.mp4", XtreamClient.episodeStreamUrl(src, "3", ""))
    }
}

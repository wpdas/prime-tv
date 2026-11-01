package com.wpdas.primetv.data.remote

import com.wpdas.primetv.data.remote.xtream.XtreamClient
import com.wpdas.primetv.domain.model.PlaylistError
import com.wpdas.primetv.domain.model.PlaylistException
import com.wpdas.primetv.domain.model.PlaylistSource
import com.wpdas.primetv.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
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

class PlaylistDetectorTest {
    private val server = MockWebServer()
    private lateinit var detector: PlaylistDetector
    private fun fixture(name: String) = javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().readText()

    @Before fun setUp() {
        server.start()
        val http = OkHttpClient()
        val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }
        detector = PlaylistDetector(http, XtreamClient(http, json), Dispatchers.Unconfined)
    }

    @After fun tearDown() = server.shutdown()

    private fun serve(block: (RecordedRequest) -> MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = block(request)
        }
    }

    @Test fun `url com usuario e senha que autentica e Xtream`() = runTest {
        serve { req ->
            if (req.path!!.startsWith("/player_api.php")) MockResponse().setBody(fixture("xtream/auth_ok.json"))
            else MockResponse().setResponseCode(404)
        }
        val result = detector.detect(server.url("/get.php?username=usuario&password=senha&type=m3u_plus").toString())
        val src = result.source as PlaylistSource.Xtream
        assertEquals(SourceType.XTREAM, src.type)
        assertEquals("usuario", src.username)
        assertEquals("senha", src.password)
        assertEquals(server.url("/").toString().trimEnd('/'), src.baseUrl)
        assertTrue(result.hasEpg)
    }

    @Test fun `auth 0 vira credenciais invalidas`() = runTest {
        serve { MockResponse().setBody(fixture("xtream/auth_fail.json")) }
        try {
            detector.detect(server.url("/get.php?username=u&password=p").toString())
            fail()
        } catch (e: PlaylistException) {
            assertEquals(PlaylistError.InvalidCredentials, e.error)
        }
    }

    @Test fun `sem player_api cai para M3U e le o epg do cabecalho`() = runTest {
        serve { req ->
            if (req.path!!.startsWith("/player_api.php")) MockResponse().setResponseCode(200).setBody("<html>not found</html>")
            else MockResponse().setBody(fixture("m3u/sample.m3u"))
        }
        val result = detector.detect(server.url("/lista.m3u?username=u&password=p").toString())
        val src = result.source as PlaylistSource.M3u
        assertEquals(SourceType.M3U, src.type)
        assertEquals("http://epg.exemplo.com/guia.xml.gz", src.epgUrl)
        assertTrue(result.hasEpg)
    }

    @Test fun `m3u simples sem credenciais`() = runTest {
        serve { MockResponse().setBody("#EXTM3U\n#EXTINF:-1,Canal\nhttp://x/y\n") }
        val result = detector.detect(server.url("/lista.m3u").toString())
        assertEquals(SourceType.M3U, result.source.type)
        assertNull((result.source as PlaylistSource.M3u).epgUrl)
    }

    @Test fun `conteudo que nao e m3u vira InvalidPlaylist`() = runTest {
        serve { MockResponse().setBody("<html>pagina</html>") }
        try { detector.detect(server.url("/qualquer").toString()); fail() } catch (e: PlaylistException) {
            assertEquals(PlaylistError.InvalidPlaylist, e.error)
        }
    }

    @Test fun `url invalida e url sem esquema`() = runTest {
        try { detector.detect("   "); fail() } catch (e: PlaylistException) { assertEquals(PlaylistError.InvalidUrl, e.error) }
        assertEquals("http://servidor.exemplo:8080/get.php", PlaylistDetector.normalizeUrl(" servidor.exemplo:8080/get.php ").toString())
        assertEquals("http://servidor.exemplo:8080", PlaylistDetector.baseOf(PlaylistDetector.normalizeUrl("http://servidor.exemplo:8080/get.php?x=1")!!))
        assertEquals("http://servidor.exemplo", PlaylistDetector.baseOf(PlaylistDetector.normalizeUrl("http://servidor.exemplo/get.php")!!))
    }
}

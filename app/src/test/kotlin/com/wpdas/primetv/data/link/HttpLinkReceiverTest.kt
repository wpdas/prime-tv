package com.wpdas.primetv.data.link

import com.wpdas.primetv.domain.link.LinkEvent
import com.wpdas.primetv.domain.link.LinkException
import com.wpdas.primetv.domain.link.LinkSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class HttpLinkReceiverTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }
    private lateinit var api: LinkApi

    // Token e URL inventados; a URL imita o formato real (usuário e senha no endereço).
    private val token = "tok-segredo-da-tv"
    private val link = "http://exemplo.test/get.php?username=fulano&password=segredo&type=m3u_plus"
    private val session = LinkSession(code = "K7P4QX", expiresAt = 9_999_999_999_000, url = "https://site.test/link?code=K7P4QX", deviceToken = token)

    @Before fun setUp() {
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LinkApi::class.java)
    }

    /** Despachante no mesmo agendador do `runTest`: os 3 s entre consultas passam em tempo virtual. */
    private fun TestScope.receiver() = HttpLinkReceiver(api, json, UnconfinedTestDispatcher(testScheduler))

    @After fun tearDown() = server.shutdown()

    private fun jsonResponse(code: Int, body: String) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)
    private fun pending() = jsonResponse(200, """{"status":"pending","expiresAt":9999999999000}""")
    private fun delivered() = jsonResponse(200, """{"status":"delivered","url":"$link","expiresAt":9999999999000}""")
    private fun RecordedRequest.auth() = getHeader("Authorization")

    @Test fun `cria a sessao e monta a url do qr com o codigo`() = runTest {
        server.enqueue(jsonResponse(201, """{"code":"K7P4QX","deviceToken":"$token","expiresAt":1700000600000,"siteUrl":"https://site.test/link"}"""))

        val created = receiver().newSession()

        assertEquals("K7P4QX", created.code)
        assertEquals(token, created.deviceToken)
        assertEquals(1700000600000, created.expiresAt)
        assertEquals("https://site.test/link?code=K7P4QX", created.url)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/sessions", request.path)
    }

    @Test fun `falha ao criar traz a mensagem do servidor`() = runTest {
        server.enqueue(jsonResponse(429, """{"error":"rate_limited","message":"Muitas tentativas. Aguarde um minuto e tente de novo."}"""))
        try {
            receiver().newSession()
            fail("devia lançar")
        } catch (e: LinkException) {
            assertEquals("Muitas tentativas. Aguarde um minuto e tente de novo.", e.userMessage)
            assertEquals("receive.error_server", e.messageKey)
            assertEquals("HTTP 429", e.detail)
        }
    }

    @Test fun `host desconhecido vira erro de dns`() = runTest {
        val api = Retrofit.Builder()
            .baseUrl("http://nao-existe.invalid/")
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LinkApi::class.java)
        try {
            HttpLinkReceiver(api, json, UnconfinedTestDispatcher(testScheduler)).newSession()
            fail("devia lançar")
        } catch (e: LinkException) {
            assertNull(e.userMessage)
            assertEquals("receive.error_dns", e.messageKey)
        }
    }

    @Test fun `sem rede ao criar lanca sem mensagem`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        try {
            receiver().newSession()
            fail("devia lançar")
        } catch (e: LinkException) {
            assertNull(e.userMessage)
            assertEquals("receive.error_server", e.messageKey)
            assertNotNull(e.detail)
        }
    }

    @Test fun `consulta ate delivered, entrega a url e confirma com DELETE`() = runTest {
        server.enqueue(pending())
        server.enqueue(pending())
        server.enqueue(delivered())
        server.enqueue(MockResponse().setResponseCode(204))

        val events = receiver().events(session).toList()

        assertEquals(listOf(LinkEvent.Waiting, LinkEvent.Received(link)), events)
        repeat(3) {
            val poll = server.takeRequest()
            assertEquals("GET", poll.method)
            assertEquals("/api/sessions/K7P4QX", poll.path)
            assertEquals("Bearer $token", poll.auth())
        }
        val ack = server.takeRequest()
        assertEquals("DELETE", ack.method)
        assertEquals("/api/sessions/K7P4QX", ack.path)
        assertEquals("Bearer $token", ack.auth())
    }

    @Test fun `expired encerra a espera`() = runTest {
        server.enqueue(pending())
        server.enqueue(jsonResponse(200, """{"status":"expired"}"""))

        assertEquals(listOf(LinkEvent.Waiting, LinkEvent.Expired), receiver().events(session).toList())
        assertEquals(2, server.requestCount)
    }

    @Test fun `delivered sem url conta como expirada`() = runTest {
        server.enqueue(jsonResponse(200, """{"status":"delivered","expiresAt":9999999999000}"""))
        assertEquals(listOf(LinkEvent.Waiting, LinkEvent.Expired), receiver().events(session).toList())
    }

    @Test fun `401 encerra como expirada`() = runTest {
        server.enqueue(jsonResponse(401, """{"error":"unauthorized","message":"Sessão não reconhecida. Gere um novo código na TV."}"""))
        assertEquals(listOf(LinkEvent.Waiting, LinkEvent.Expired), receiver().events(session).toList())
        assertEquals(1, server.requestCount)
    }

    @Test fun `429 espera e segue consultando ate receber`() = runTest {
        server.enqueue(jsonResponse(429, """{"error":"rate_limited","message":"Muitas tentativas."}"""))
        server.enqueue(delivered())
        server.enqueue(MockResponse().setResponseCode(204))

        val events = receiver().events(session).toList()

        assertEquals(listOf(LinkEvent.Waiting, LinkEvent.Received(link)), events)
    }

    @Test fun `falha de rede avisa offline, volta a aguardar e recebe`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(pending())
        server.enqueue(delivered())
        server.enqueue(MockResponse().setResponseCode(204))

        val events = receiver().events(session).toList()

        assertEquals(listOf(LinkEvent.Waiting, LinkEvent.Offline, LinkEvent.Waiting, LinkEvent.Received(link)), events)
    }

    @Test fun `release manda DELETE com o token e engole erro`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        receiver().release(session)
        val ack = server.takeRequest()
        assertEquals("DELETE", ack.method)
        assertEquals("Bearer $token", ack.auth())
        assertNotNull(ack.path)
    }

    @Test fun `release sem token nao chama o servidor`() = runTest {
        receiver().release(session.copy(deviceToken = ""))
        assertEquals(0, server.requestCount)
    }

    @Test fun `origem da api e url do qr a partir do site`() {
        assertEquals("https://prime-tv-web.wendersonpdas.workers.dev/", LinkSites.origin("https://prime-tv-web.wendersonpdas.workers.dev/link"))
        assertEquals("http://localhost:5173/", LinkSites.origin("http://localhost:5173/link?x=1"))
        assertEquals("https://site.test/", LinkSites.origin("https://site.test"))
        assertEquals("https://site.test/link?code=ABCDEF", LinkSites.qrUrl("https://site.test/link", "ABCDEF"))
        assertTrue(LinkCodes.urlFor("K7P4QX").endsWith("/link?code=K7P4QX"))
    }
}

package com.example.poprogknowledgebaseback

import com.example.poprogknowledgebaseback.adapters.outbound.assistant.gigachat.GigaChatAiAssistantAdapter
import com.example.poprogknowledgebaseback.adapters.outbound.assistant.gigachat.GigaChatTokenProvider
import com.example.poprogknowledgebaseback.config.GigaChatProperties
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

class GigaChatAiAssistantAdapterTest {

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should request oauth token once and reuse it for multiple calls`() {
        server.enqueue(jsonResponse("""{"access_token":"token-1","expires_at":4102444800}"""))

        val tokenProvider = createTokenProvider()

        assertEquals("token-1", tokenProvider.getAccessToken())
        assertEquals("token-1", tokenProvider.getAccessToken())

        val oauthRequest = server.takeRequest()
        assertEquals("/api/v2/oauth", oauthRequest.path)
        assertEquals("POST", oauthRequest.method)
        assertEquals("Basic test-auth-key", oauthRequest.getHeader(HttpHeaders.AUTHORIZATION))
        assertEquals("scope=GIGACHAT_API_PERS", oauthRequest.body.readUtf8())
        assertNotNull(oauthRequest.getHeader("RqUID"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `should call gigachat chat completions with bearer token and mapped messages`() {
        server.enqueue(jsonResponse("""{"access_token":"token-2","expires_at":4102444800}"""))
        server.enqueue(
            jsonResponse(
                """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "Привет! Чем могу помочь?"
                      },
                      "index": 0,
                      "finish_reason": "stop"
                    }
                  ],
                  "created": 1743077275,
                  "model": "GigaChat-Pro",
                  "usage": {
                    "prompt_tokens": 12,
                    "completion_tokens": 6,
                    "total_tokens": 18
                  }
                }
                """.trimIndent()
            )
        )

        val properties = gigaChatProperties()
        val tokenProvider = createTokenProvider(properties)
        val adapter = GigaChatAiAssistantAdapter(
            gigaChatApiRestClient = RestClient.builder()
                .baseUrl(server.url("/").toString().removeSuffix("/"))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build(),
            gigaChatTokenProvider = tokenProvider,
            gigaChatProperties = properties.copy(model = "GigaChat-Pro")
        )

        val response = adapter.complete(
            listOf(
                AiChatMessage(AiChatMessageRole.SYSTEM, "Отвечай кратко"),
                AiChatMessage(AiChatMessageRole.USER, "Привет")
            )
        )

        assertEquals("Привет! Чем могу помочь?", response.content)
        assertEquals("GigaChat-Pro", response.model)
        assertEquals("stop", response.finishReason)
        assertEquals(12, response.promptTokens)
        assertEquals(6, response.completionTokens)
        assertEquals(18, response.totalTokens)

        val oauthRequest = server.takeRequest()
        assertEquals("/api/v2/oauth", oauthRequest.path)

        val chatRequest = server.takeRequest()
        assertEquals("/api/v1/chat/completions", chatRequest.path)
        assertEquals("Bearer token-2", chatRequest.getHeader(HttpHeaders.AUTHORIZATION))
        val body = chatRequest.body.readUtf8()
        assert(body.contains("GigaChat-Pro"))
        assert(body.contains("\"role\":\"system\""))
        assert(body.contains("\"role\":\"user\""))
        assert(body.contains("Привет"))
    }

    @Test
    fun `should refresh token and retry when gigachat returns unauthorized`() {
        server.enqueue(jsonResponse("""{"access_token":"expired-token","expires_at":4102444800}"""))
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""{"status":401,"message":"Token has expired"}""")
        )
        server.enqueue(jsonResponse("""{"access_token":"fresh-token","expires_at":4102444800}"""))
        server.enqueue(
            jsonResponse(
                """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "Свежий ответ"
                      },
                      "index": 0,
                      "finish_reason": "stop"
                    }
                  ],
                  "model": "GigaChat",
                  "usage": {
                    "prompt_tokens": 4,
                    "completion_tokens": 2,
                    "total_tokens": 6
                  }
                }
                """.trimIndent()
            )
        )

        val properties = gigaChatProperties()
        val tokenProvider = createTokenProvider(properties)
        val adapter = GigaChatAiAssistantAdapter(
            gigaChatApiRestClient = RestClient.builder()
                .baseUrl(server.url("/").toString().removeSuffix("/"))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build(),
            gigaChatTokenProvider = tokenProvider,
            gigaChatProperties = properties
        )

        val response = adapter.complete(
            listOf(AiChatMessage(AiChatMessageRole.USER, "Проверка"))
        )

        assertEquals("Свежий ответ", response.content)

        val oauthRequest1 = server.takeRequest()
        assertEquals("/api/v2/oauth", oauthRequest1.path)

        val chatRequest1 = server.takeRequest()
        assertEquals("/api/v1/chat/completions", chatRequest1.path)
        assertEquals("Bearer expired-token", chatRequest1.getHeader(HttpHeaders.AUTHORIZATION))

        val oauthRequest2 = server.takeRequest()
        assertEquals("/api/v2/oauth", oauthRequest2.path)

        val chatRequest2 = server.takeRequest()
        assertEquals("/api/v1/chat/completions", chatRequest2.path)
        assertEquals("Bearer fresh-token", chatRequest2.getHeader(HttpHeaders.AUTHORIZATION))
    }

    private fun createTokenProvider(
        properties: GigaChatProperties = gigaChatProperties()
    ): GigaChatTokenProvider = GigaChatTokenProvider(
        gigaChatAuthRestClient = RestClient.builder()
            .baseUrl(server.url("/").toString().removeSuffix("/"))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .build(),
        gigaChatProperties = properties,
        clock = Clock.fixed(Instant.parse("2026-04-04T00:00:00Z"), ZoneOffset.UTC)
    )

    private fun gigaChatProperties() = GigaChatProperties(
        enabled = true,
        authUrl = server.url("/").toString().removeSuffix("/"),
        apiUrl = server.url("/").toString().removeSuffix("/"),
        authorizationKey = "test-auth-key",
        scope = "GIGACHAT_API_PERS",
        model = "GigaChat"
    )

    private fun jsonResponse(body: String) = MockResponse()
        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .setBody(body)
}

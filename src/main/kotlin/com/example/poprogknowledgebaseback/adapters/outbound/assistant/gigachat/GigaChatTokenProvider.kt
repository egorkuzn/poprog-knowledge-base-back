package com.example.poprogknowledgebaseback.adapters.outbound.assistant.gigachat

import com.fasterxml.jackson.annotation.JsonProperty
import com.example.poprogknowledgebaseback.config.GigaChatProperties
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "true")
class GigaChatTokenProvider(
    private val gigaChatAuthRestClient: RestClient,
    private val gigaChatProperties: GigaChatProperties,
    private val clock: Clock
) {

    @Volatile
    private var cachedToken: CachedToken? = null

    fun getAccessToken(): String {
        val token = cachedToken
        if (token != null && token.isValidAt(clock.instant())) {
            return token.value
        }

        synchronized(this) {
            val actualToken = cachedToken
            if (actualToken != null && actualToken.isValidAt(clock.instant())) {
                return actualToken.value
            }

            val refreshedToken = requestToken()
            cachedToken = refreshedToken
            return refreshedToken.value
        }
    }

    private fun requestToken(): CachedToken {
        require(gigaChatProperties.authorizationKey.isNotBlank()) {
            "GigaChat authorization key is not configured"
        }

        val response = gigaChatAuthRestClient.post()
            .uri("/api/v2/oauth")
            .headers { headers ->
                headers.set(HttpHeaders.AUTHORIZATION, gigaChatProperties.authorizationHeader())
                headers.set("RqUID", UUID.randomUUID().toString())
            }
            .body("scope=${gigaChatProperties.scope}")
            .retrieve()
            .body(GigaChatOAuthResponse::class.java)
            ?: error("GigaChat returned an empty OAuth response")

        return CachedToken(
            value = response.accessToken,
            expiresAt = Instant.ofEpochSecond(response.expiresAt)
        )
    }

    private fun GigaChatProperties.authorizationHeader(): String =
        authorizationKey.takeIf { it.startsWith("Basic ") } ?: "Basic $authorizationKey"

    private data class CachedToken(
        val value: String,
        val expiresAt: Instant
    ) {
        fun isValidAt(now: Instant): Boolean = expiresAt.minusSeconds(60).isAfter(now)
    }
}

data class GigaChatOAuthResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_at")
    val expiresAt: Long
)

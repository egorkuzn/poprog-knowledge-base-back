package com.example.poprogknowledgebaseback

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.KeycloakJwtCurrentUserProvider
import com.example.poprogknowledgebaseback.config.AuthKeycloakProperties
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import java.security.KeyPairGenerator
import java.time.Instant
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.web.server.ResponseStatusException

class KeycloakJwtCurrentUserProviderTest {

    @Test
    fun `should resolve current user from keycloak access token`() {
        val rsaKey = generateRsaKey()
        MockWebServer().use { server ->
            server.enqueue(jwksResponse(rsaKey))
            server.start()

            val provider = KeycloakJwtCurrentUserProvider(
                AuthKeycloakProperties().apply {
                    enabled = true
                    jwkSetUri = server.url("/realms/reflex-ide/protocol/openid-connect/certs").toString()
                    clientId = "reflex-web-client"
                    requiredAudience = "reflex-web-client"
                }
            )
            val token = encodeJwt(
                rsaKey = rsaKey,
                audience = listOf("reflex-web-client"),
                claims = mapOf(
                    "sub" to "keycloak-sub-1",
                    "email" to "admin@poprog.local",
                    "name" to "Portal Admin",
                    "realm_access" to mapOf("roles" to listOf("offline_access")),
                    "resource_access" to mapOf(
                        "reflex-web-client" to mapOf("roles" to listOf("admin", "editor"))
                    )
                )
            )

            val request = MockHttpServletRequest().apply {
                addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
            }

            val user = provider.resolveOrNull(request)

            checkNotNull(user)
            assertEquals("keycloak-sub-1", user.subject)
            assertEquals("admin@poprog.local", user.email)
            assertEquals("Portal Admin", user.name)
            assertEquals(setOf("USER", "OFFLINE_ACCESS", "ADMIN", "EDITOR"), user.roles)
        }
    }

    @Test
    fun `should reject token with unexpected audience`() {
        val rsaKey = generateRsaKey()
        MockWebServer().use { server ->
            server.enqueue(jwksResponse(rsaKey))
            server.start()

            val provider = KeycloakJwtCurrentUserProvider(
                AuthKeycloakProperties().apply {
                    enabled = true
                    jwkSetUri = server.url("/certs").toString()
                    requiredAudience = "reflex-web-client"
                }
            )
            val token = encodeJwt(
                rsaKey = rsaKey,
                audience = listOf("another-client"),
                claims = mapOf("sub" to "user-1")
            )
            val request = MockHttpServletRequest().apply {
                addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
            }

            val ex = assertThrows<ResponseStatusException> {
                provider.resolveOrNull(request)
            }

            assertEquals(401, ex.statusCode.value())
        }
    }

    @Test
    fun `should accept token when authorized party matches required audience`() {
        val rsaKey = generateRsaKey()
        MockWebServer().use { server ->
            server.enqueue(jwksResponse(rsaKey))
            server.start()

            val provider = KeycloakJwtCurrentUserProvider(
                AuthKeycloakProperties().apply {
                    enabled = true
                    jwkSetUri = server.url("/certs").toString()
                    requiredAudience = "reflex-web-client"
                }
            )
            val token = encodeJwt(
                rsaKey = rsaKey,
                audience = listOf("account"),
                claims = mapOf(
                    "sub" to "user-azp",
                    "azp" to "reflex-web-client"
                )
            )
            val request = MockHttpServletRequest().apply {
                addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
            }

            val user = provider.resolveOrNull(request)

            checkNotNull(user)
            assertEquals("user-azp", user.subject)
        }
    }


    private fun generateRsaKey(): RSAKey {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.generateKeyPair()

        return RSAKey.Builder(keyPair.public as java.security.interfaces.RSAPublicKey)
            .privateKey(keyPair.private)
            .keyID("test-key")
            .build()
    }

    private fun jwksResponse(rsaKey: RSAKey): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(JWKSet(rsaKey.toPublicJWK()).toString())

    private fun encodeJwt(
        rsaKey: RSAKey,
        audience: List<String>,
        claims: Map<String, Any>
    ): String {
        val encoder = NimbusJwtEncoder(ImmutableJWKSet(JWKSet(rsaKey)))
        val now = Instant.now()
        val claimsBuilder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(now.plusSeconds(600))
            .audience(audience)

        claims.forEach { (name, value) -> claimsBuilder.claim(name, value) }

        val headers = JwsHeader.with(SignatureAlgorithm.RS256)
            .keyId(rsaKey.keyID)
            .build()

        return encoder.encode(JwtEncoderParameters.from(headers, claimsBuilder.build())).tokenValue
    }
}

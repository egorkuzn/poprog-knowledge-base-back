package com.example.poprogknowledgebaseback.adapters.inbound.web.auth

import com.example.poprogknowledgebaseback.config.AuthKeycloakProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class KeycloakJwtCurrentUserProvider(
    private val properties: AuthKeycloakProperties
) : CurrentUserProvider {

    private val decoder: JwtDecoder? by lazy { createDecoderOrNull() }

    override fun resolveOrNull(request: HttpServletRequest): CurrentUser? {
        if (!properties.enabled) {
            return null
        }

        val token = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.trim()
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(" ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val jwt = try {
            val activeDecoder = decoder
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak JWT decoder is not configured")
            activeDecoder.decode(token)
        } catch (ex: JwtException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token", ex)
        }

        validateAudience(jwt)

        return CurrentUser(
            subject = jwt.subject,
            email = jwt.claimAsString("email"),
            name = jwt.claimAsString("name") ?: jwt.claimAsString("preferred_username"),
            roles = resolveRoles(jwt)
        )
    }

    private fun createDecoderOrNull(): JwtDecoder? {
        val jwkSetUri = properties.jwkSetUri.trim()
        if (jwkSetUri.isNotBlank()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
        }

        val issuerUri = properties.issuerUri.trim()
        if (issuerUri.isNotBlank()) {
            return JwtDecoders.fromIssuerLocation(issuerUri)
        }

        return null
    }

    private fun validateAudience(jwt: Jwt) {
        val requiredAudience = properties.requiredAudience.trim()
        if (requiredAudience.isBlank()) {
            return
        }

        val authorizedParty = jwt.claimAsString("azp")
        if (!jwt.audience.contains(requiredAudience) && authorizedParty != requiredAudience) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token audience is not allowed")
        }
    }

    private fun resolveRoles(jwt: Jwt): Set<String> {
        val roles = linkedSetOf("USER")
        roles += rolesFromMap(jwt.claimAsStringObjectMap("realm_access"))

        val clientId = properties.clientId.trim()
        val resourceAccess = jwt.claimAsStringObjectMap("resource_access")
        val clientAccess = resourceAccess?.get(clientId) as? Map<*, *>
        roles += rolesFromMap(clientAccess)

        roles += jwt.claimAsString("scope")
            ?.split(" ")
            ?.mapNotNull { normalizeRole(it) }
            .orEmpty()

        @Suppress("UNCHECKED_CAST")
        roles += (jwt.claims["scp"] as? Collection<String>)
            ?.mapNotNull { normalizeRole(it) }
            .orEmpty()

        return roles
    }

    private fun rolesFromMap(access: Map<*, *>?): Set<String> {
        @Suppress("UNCHECKED_CAST")
        return (access?.get("roles") as? Collection<String>)
            ?.mapNotNull { normalizeRole(it) }
            ?.toSet()
            ?: emptySet()
    }

    private fun normalizeRole(raw: String): String? {
        val normalized = raw
            .trim()
            .removePrefix("ROLE_")
            .uppercase()

        return normalized.takeIf { it.isNotBlank() }
    }

    private fun Jwt.claimAsString(name: String): String? =
        claims[name]
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun Jwt.claimAsStringObjectMap(name: String): Map<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return claims[name] as? Map<String, Any>
    }
}

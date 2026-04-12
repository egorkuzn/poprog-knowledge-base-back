package com.example.poprogknowledgebaseback.adapters.inbound.web.auth

import com.example.poprogknowledgebaseback.config.AuthDevHeadersProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class DevHeadersCurrentUserProvider(
    private val properties: AuthDevHeadersProperties,
    private val environment: Environment
) : CurrentUserProvider {

    override fun resolveOrNull(request: HttpServletRequest): CurrentUser? {
        if (!properties.enabled) {
            return null
        }
        ensureAllowedProfile()

        val subject = request.headerValue(properties.subjectHeader) ?: return null
        val email = request.headerValue(properties.emailHeader)
        val name = request.headerValue(properties.nameHeader)
        val roles = parseRoles(request.headerValue(properties.rolesHeader))

        return CurrentUser(
            subject = subject,
            email = email,
            name = name,
            roles = roles
        )
    }

    private fun ensureAllowedProfile() {
        val activeProfiles = environment.activeProfiles.map { it.lowercase() }.toSet()
        val allowedProfiles = properties.allowedProfiles.map { it.lowercase() }.toSet()
        if (activeProfiles.isEmpty() || activeProfiles.intersect(allowedProfiles).isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Dev header auth can be used only in allowed local/dev profiles"
            )
        }
    }

    private fun parseRoles(raw: String?): Set<String> =
        raw
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.map { it.uppercase() }
            ?.toSet()
            ?: emptySet()

    private fun HttpServletRequest.headerValue(name: String): String? =
        getHeader(name)?.trim()?.takeIf { it.isNotBlank() }
}

package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.config.AuthKeycloakProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AccountPasswordResetService(
    private val properties: AuthKeycloakProperties,
    private val keycloakUserAdminClient: KeycloakUserAdminClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun requestReset(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            return
        }

        if (!properties.enabled) {
            log.warn("Password reset requested while Keycloak integration is disabled")
            return
        }

        try {
            keycloakUserAdminClient.sendPasswordResetEmail(normalizedEmail)
        } catch (error: Exception) {
            // Deliberately swallow details to prevent account enumeration.
            log.warn("Password reset request was not completed for email={}", normalizedEmail, error)
        }
    }
}

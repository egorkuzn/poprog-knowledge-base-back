package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.account.SpringDataUserAccountRepository
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.account.UserAccountJpaEntity
import com.example.poprogknowledgebaseback.config.AuthKeycloakProperties
import java.time.Clock
import java.time.OffsetDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

data class RegisterAccountCommand(
    val name: String,
    val email: String,
    val password: String
)

interface KeycloakUserAdminClient {
    fun createUser(command: RegisterAccountCommand): String
}

@Service
class KeycloakAccountRegistrationService(
    private val properties: AuthKeycloakProperties,
    private val keycloakUserAdminClient: KeycloakUserAdminClient,
    private val userAccountRepository: SpringDataUserAccountRepository,
    private val clock: Clock
) {

    @Transactional
    fun register(command: RegisterAccountCommand): AccountProfileResult {
        if (!properties.enabled) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak registration is disabled")
        }

        val normalizedName = command.name.trim()
        val normalizedEmail = command.email.trim().lowercase()

        userAccountRepository.findByEmail(normalizedEmail).ifPresent {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists")
        }

        val keycloakSub = keycloakUserAdminClient.createUser(
            command.copy(
                name = normalizedName,
                email = normalizedEmail,
                password = command.password.trim()
            )
        )

        val now = OffsetDateTime.now(clock)
        val stored = userAccountRepository.save(
            UserAccountJpaEntity(
                keycloakSub = keycloakSub,
                name = normalizedName,
                email = normalizedEmail,
                createdAt = now,
                updatedAt = now
            )
        )

        return AccountProfileResult(
            subject = stored.keycloakSub,
            name = stored.name,
            email = stored.email,
            roles = setOf("USER")
        )
    }
}

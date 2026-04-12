package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.account.SpringDataUserAccountRepository
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.account.UserAccountJpaEntity
import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.CurrentUser
import java.time.Clock
import java.time.OffsetDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AccountProfileResult(
    val subject: String,
    val name: String,
    val email: String,
    val roles: Set<String>
)

data class UpdateAccountProfileCommand(
    val name: String,
    val email: String
)

@Service
class AccountProfileService(
    private val userAccountRepository: SpringDataUserAccountRepository,
    private val clock: Clock
) {

    @Transactional
    fun getOrCreateProfile(currentUser: CurrentUser): AccountProfileResult {
        val existing = userAccountRepository.findByKeycloakSub(currentUser.subject).orElse(null)
        val stored = if (existing == null) {
            createUser(currentUser)
        } else {
            existing
        }

        return stored.toResult(currentUser.roles)
    }

    @Transactional
    fun updateProfile(currentUser: CurrentUser, command: UpdateAccountProfileCommand): AccountProfileResult {
        val stored = userAccountRepository.findByKeycloakSub(currentUser.subject).orElseGet {
            createUser(currentUser)
        }

        stored.name = command.name.trim()
        stored.email = command.email.trim().lowercase()
        stored.updatedAt = OffsetDateTime.now(clock)

        return userAccountRepository.save(stored).toResult(currentUser.roles)
    }

    private fun createUser(currentUser: CurrentUser): UserAccountJpaEntity {
        val now = OffsetDateTime.now(clock)
        val normalizedName = currentUser.name?.trim().takeUnless { it.isNullOrBlank() } ?: "Local User"
        val normalizedEmail = currentUser.email?.trim()?.lowercase().takeUnless { it.isNullOrBlank() }
            ?: "${currentUser.subject}@unknown.local"
        return userAccountRepository.save(
            UserAccountJpaEntity(
                keycloakSub = currentUser.subject,
                name = normalizedName,
                email = normalizedEmail,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun UserAccountJpaEntity.toResult(roles: Set<String>) = AccountProfileResult(
        subject = keycloakSub,
        name = name,
        email = email,
        roles = roles
    )
}

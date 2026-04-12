package com.example.poprogknowledgebaseback.adapters.outbound.persistence.account

import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataUserAccountRepository : JpaRepository<UserAccountJpaEntity, Long> {
    fun findByKeycloakSub(keycloakSub: String): Optional<UserAccountJpaEntity>
}

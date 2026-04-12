package com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation

import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataDonationPaymentRepository : JpaRepository<DonationPaymentJpaEntity, UUID> {
    fun findAllByUserSubOrderByCreatedAtDesc(userSub: String): List<DonationPaymentJpaEntity>
    fun findByIdAndUserSub(id: UUID, userSub: String): Optional<DonationPaymentJpaEntity>
}

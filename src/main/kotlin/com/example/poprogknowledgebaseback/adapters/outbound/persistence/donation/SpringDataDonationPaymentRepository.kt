package com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation

import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataDonationPaymentRepository : JpaRepository<DonationPaymentJpaEntity, UUID> {
    fun findAllByUserSubOrderByCreatedAtDesc(userSub: String): List<DonationPaymentJpaEntity>
    fun findByIdAndUserSub(id: UUID, userSub: String): Optional<DonationPaymentJpaEntity>
    fun findAllByUserSubAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
        userSub: String,
        from: java.time.OffsetDateTime
    ): List<DonationPaymentJpaEntity>
    fun findAllByUserSubAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
        userSub: String,
        to: java.time.OffsetDateTime
    ): List<DonationPaymentJpaEntity>
    fun findAllByUserSubAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
        userSub: String,
        from: java.time.OffsetDateTime,
        to: java.time.OffsetDateTime
    ): List<DonationPaymentJpaEntity>
    fun findAllByOrderByCreatedAtDesc(): List<DonationPaymentJpaEntity>
    fun findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(from: java.time.OffsetDateTime): List<DonationPaymentJpaEntity>
    fun findAllByCreatedAtLessThanEqualOrderByCreatedAtDesc(to: java.time.OffsetDateTime): List<DonationPaymentJpaEntity>
    fun findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
        from: java.time.OffsetDateTime,
        to: java.time.OffsetDateTime
    ): List<DonationPaymentJpaEntity>
}

package com.example.poprogknowledgebaseback.adapters.outbound.persistence.donation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "donation_payment")
class DonationPaymentJpaEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "user_sub", length = 128)
    var userSub: String?,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(nullable = false, length = 32)
    var status: String,

    @Column(length = 255)
    var source: String?,

    @Column(length = 1000)
    var message: String?,

    @Column(name = "provider_payment_id", length = 128)
    var providerPaymentId: String?,

    @Column(name = "confirmation_url", length = 4096)
    var confirmationUrl: String?,

    @Column(name = "return_url", nullable = false, length = 2048)
    var returnUrl: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime,

    @Column(name = "paid_at")
    var paidAt: OffsetDateTime?
)

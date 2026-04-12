package com.example.poprogknowledgebaseback.adapters.outbound.persistence.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "user_account")
class UserAccountJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "keycloak_sub", nullable = false, unique = true, length = 128)
    var keycloakSub: String,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false, length = 254)
    var email: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime
)

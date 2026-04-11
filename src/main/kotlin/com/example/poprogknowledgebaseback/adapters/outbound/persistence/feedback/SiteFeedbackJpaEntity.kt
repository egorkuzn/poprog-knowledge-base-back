package com.example.poprogknowledgebaseback.adapters.outbound.persistence.feedback

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "site_feedback")
class SiteFeedbackJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var helpful: Boolean,

    @Column
    var source: String? = null,

    @Column(columnDefinition = "text")
    var comment: String? = null,

    @Column(name = "user_name")
    var userName: String? = null,

    @Column(name = "user_email")
    var userEmail: String? = null,

    @Column(name = "user_agent")
    var userAgent: String? = null,

    @Column(name = "ip_address")
    var ipAddress: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)

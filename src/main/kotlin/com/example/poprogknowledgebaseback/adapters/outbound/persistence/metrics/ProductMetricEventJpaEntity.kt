package com.example.poprogknowledgebaseback.adapters.outbound.persistence.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "product_metric_event")
class ProductMetricEventJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "event_type", nullable = false, length = 64)
    var eventType: String,

    @Column(nullable = false, length = 255)
    var route: String,

    @Column(length = 2048)
    var referrer: String? = null,

    @Column(name = "session_id", nullable = false, length = 128)
    var sessionId: String,

    @Column(name = "user_key", nullable = false, length = 128)
    var userKey: String,

    @Column(name = "timestamp_client", nullable = false)
    var timestampClient: OffsetDateTime,

    @Column(name = "timestamp_server", nullable = false)
    var timestampServer: OffsetDateTime,

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    var payloadJson: String
)

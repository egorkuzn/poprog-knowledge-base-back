package com.example.poprogknowledgebaseback.adapters.outbound.persistence.metrics

import java.time.OffsetDateTime
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataProductMetricEventRepository : JpaRepository<ProductMetricEventJpaEntity, Long> {
    fun findAllByTimestampServerGreaterThanEqualAndTimestampServerLessThanEqualOrderByTimestampServerAsc(
        from: OffsetDateTime,
        to: OffsetDateTime
    ): List<ProductMetricEventJpaEntity>
}

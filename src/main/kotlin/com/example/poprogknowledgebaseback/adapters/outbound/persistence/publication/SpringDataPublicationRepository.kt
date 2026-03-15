package com.example.poprogknowledgebaseback.adapters.outbound.persistence.publication

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataPublicationRepository : JpaRepository<PublicationJpaEntity, Long> {
    fun findAllByOrderByYearDescIdAsc(): List<PublicationJpaEntity>
}

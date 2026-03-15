package com.example.poprogknowledgebaseback.adapters.outbound.persistence.studentwork

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataProjectTypeRepository : JpaRepository<ProjectTypeJpaEntity, Long> {
    fun findByHash(hash: String): ProjectTypeJpaEntity?
}

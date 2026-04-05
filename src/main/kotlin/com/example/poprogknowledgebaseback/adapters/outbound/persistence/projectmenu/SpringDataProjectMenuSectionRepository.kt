package com.example.poprogknowledgebaseback.adapters.outbound.persistence.projectmenu

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataProjectMenuSectionRepository : JpaRepository<ProjectMenuSectionJpaEntity, Long> {
    fun findAllByOrderBySortOrderAscIdAsc(): List<ProjectMenuSectionJpaEntity>
    fun findByHash(hash: String): ProjectMenuSectionJpaEntity?
}

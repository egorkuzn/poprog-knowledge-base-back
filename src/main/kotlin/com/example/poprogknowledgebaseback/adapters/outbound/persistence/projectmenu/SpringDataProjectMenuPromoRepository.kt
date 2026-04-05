package com.example.poprogknowledgebaseback.adapters.outbound.persistence.projectmenu

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataProjectMenuPromoRepository : JpaRepository<ProjectMenuPromoJpaEntity, Long> {
    fun findAllBySectionIdInOrderBySortOrderAscIdAsc(sectionIds: List<Long>): List<ProjectMenuPromoJpaEntity>
}

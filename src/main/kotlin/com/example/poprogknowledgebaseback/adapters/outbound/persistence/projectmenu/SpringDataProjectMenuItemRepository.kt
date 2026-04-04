package com.example.poprogknowledgebaseback.adapters.outbound.persistence.projectmenu

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataProjectMenuItemRepository : JpaRepository<ProjectMenuItemJpaEntity, Long> {
    fun findAllBySectionIdInOrderBySortOrderAscIdAsc(sectionIds: List<Long>): List<ProjectMenuItemJpaEntity>
}

package com.example.poprogknowledgebaseback.adapters.outbound.persistence.favorite

import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataFavoriteItemRepository : JpaRepository<FavoriteItemJpaEntity, Long> {
    fun findAllByUserSubOrderByCreatedAtDesc(userSub: String): List<FavoriteItemJpaEntity>
    fun findByUserSubAndItemTypeAndItemId(userSub: String, itemType: String, itemId: String): Optional<FavoriteItemJpaEntity>
    fun deleteByUserSubAndItemTypeAndItemId(userSub: String, itemType: String, itemId: String): Long
}

package com.example.poprogknowledgebaseback.application.account

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.favorite.FavoriteItemJpaEntity
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.favorite.SpringDataFavoriteItemRepository
import java.time.Clock
import java.time.OffsetDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AccountFavoriteResult(
    val id: Long,
    val itemType: String,
    val itemId: String,
    val title: String,
    val link: String?,
    val createdAt: String
)

data class UpsertFavoriteCommand(
    val itemType: String,
    val itemId: String,
    val title: String,
    val link: String?
)

@Service
class AccountFavoriteService(
    private val favoriteRepository: SpringDataFavoriteItemRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getFavorites(userSub: String): List<AccountFavoriteResult> =
        favoriteRepository.findAllByUserSubOrderByCreatedAtDesc(userSub)
            .map { it.toResult() }

    @Transactional
    fun upsertFavorite(userSub: String, command: UpsertFavoriteCommand): AccountFavoriteResult {
        val normalizedType = command.itemType.trim().uppercase()
        val normalizedItemId = command.itemId.trim()

        val existing = favoriteRepository.findByUserSubAndItemTypeAndItemId(userSub, normalizedType, normalizedItemId).orElse(null)
        val entity = if (existing == null) {
            FavoriteItemJpaEntity(
                userSub = userSub,
                itemType = normalizedType,
                itemId = normalizedItemId,
                title = command.title.trim(),
                link = command.link?.trim()?.takeIf { it.isNotBlank() },
                createdAt = OffsetDateTime.now(clock)
            )
        } else {
            existing.title = command.title.trim()
            existing.link = command.link?.trim()?.takeIf { it.isNotBlank() }
            existing
        }

        return favoriteRepository.save(entity).toResult()
    }

    @Transactional
    fun deleteFavorite(userSub: String, itemType: String, itemId: String) {
        favoriteRepository.deleteByUserSubAndItemTypeAndItemId(userSub, itemType.trim().uppercase(), itemId.trim())
    }

    private fun FavoriteItemJpaEntity.toResult() = AccountFavoriteResult(
        id = id ?: error("Favorite id was not generated"),
        itemType = itemType,
        itemId = itemId,
        title = title,
        link = link,
        createdAt = createdAt.toString()
    )
}

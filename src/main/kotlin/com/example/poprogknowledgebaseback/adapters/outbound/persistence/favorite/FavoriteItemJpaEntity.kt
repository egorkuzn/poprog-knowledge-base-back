package com.example.poprogknowledgebaseback.adapters.outbound.persistence.favorite

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "favorite_item")
class FavoriteItemJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_sub", nullable = false, length = 128)
    var userSub: String,

    @Column(name = "item_type", nullable = false, length = 32)
    var itemType: String,

    @Column(name = "item_id", nullable = false, length = 128)
    var itemId: String,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(length = 2048)
    var link: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime
)

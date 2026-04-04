package com.example.poprogknowledgebaseback.adapters.outbound.persistence.assistant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "chat_conversation")
class ChatConversationJpaEntity(
    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)

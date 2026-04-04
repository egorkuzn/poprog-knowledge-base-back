package com.example.poprogknowledgebaseback.adapters.outbound.persistence.assistant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole

@Entity
@Table(name = "chat_message")
class ChatMessageJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "chat_id", nullable = false, updatable = false)
    val chatId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val role: AiChatMessageRole,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)

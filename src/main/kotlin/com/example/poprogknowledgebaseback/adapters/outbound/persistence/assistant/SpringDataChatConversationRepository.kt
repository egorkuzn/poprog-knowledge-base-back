package com.example.poprogknowledgebaseback.adapters.outbound.persistence.assistant

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataChatConversationRepository : JpaRepository<ChatConversationJpaEntity, UUID>

package com.example.poprogknowledgebaseback.application.assistant

import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import java.time.Instant
import java.util.UUID

data class AssistantChatCommand(
    val chatId: UUID?,
    val messages: List<AiChatMessage>,
    val documentRef: AssistantDocumentRef? = null,
    val requesterSub: String? = null
)

data class AssistantChatResult(
    val chatId: UUID,
    val content: String,
    val model: String,
    val finishReason: String?,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?,
    val documentHints: List<AssistantDocumentHint> = emptyList(),
    val mode: AssistantResponseMode = AssistantResponseMode.TEXT,
    val widget: AssistantWidgetResult? = null
)

enum class AssistantResponseMode {
    TEXT,
    WIDGET,
    HYBRID
}

data class AssistantWidgetResult(
    val widgetType: String,
    val title: String,
    val subtitle: String? = null,
    val items: List<AssistantWidgetItemResult> = emptyList(),
    val actions: List<AssistantWidgetActionResult> = emptyList(),
    val followUpOptions: List<AssistantWidgetActionResult> = emptyList()
)

data class AssistantWidgetItemResult(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val meta: String? = null,
    val href: String? = null,
    val prompt: String? = null,
    val sourceType: String? = null,
    val sourceId: String? = null
)

data class AssistantWidgetActionResult(
    val id: String,
    val label: String,
    val kind: String,
    val href: String? = null,
    val prompt: String? = null
)

data class AssistantDocumentRef(
    val sourceType: String?,
    val sourceUuid: String?
)

data class AssistantDocumentHint(
    val sourceType: String,
    val sourceUuid: String?,
    val scoreHint: Int,
    val groupTitle: String,
    val groupHash: String?,
    val authors: String,
    val theme: String,
    val published: String,
    val link: String?,
    val snippet: String
)

data class ChatHistoryResult(
    val chatId: UUID,
    val messages: List<ChatHistoryMessageResult>
)

data class ChatHistoryMessageResult(
    val id: Long,
    val role: AiChatMessageRole,
    val content: String,
    val widget: AssistantWidgetResult? = null,
    val createdAt: Instant
)

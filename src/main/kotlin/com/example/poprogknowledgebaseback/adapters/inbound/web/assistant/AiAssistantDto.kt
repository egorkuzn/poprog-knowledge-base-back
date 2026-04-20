package com.example.poprogknowledgebaseback.adapters.inbound.web.assistant

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import java.time.Instant
import java.util.UUID

data class AiAssistantChatRequest(
    val chatId: UUID? = null,
    val document: AiAssistantDocumentRefRequest? = null,
    @field:Valid
    @field:NotEmpty
    val messages: List<AiAssistantChatMessageRequest>
)

data class AiAssistantDocumentRefRequest(
    val sourceType: String? = null,
    val sourceUuid: String? = null
)

data class AiAssistantChatMessageRequest(
    @field:Pattern(regexp = "system|user|assistant")
    val role: String,
    @field:NotBlank
    val content: String
)

data class AiAssistantChatResponse(
    val chatId: UUID,
    val content: String,
    val model: String,
    val mode: String,
    val finishReason: String?,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?,
    val documentHints: List<AiAssistantDocumentHintResponse> = emptyList(),
    val widget: AiAssistantWidgetResponse? = null
)

data class AiAssistantWidgetResponse(
    val widgetType: String,
    val title: String,
    val subtitle: String? = null,
    val items: List<AiAssistantWidgetItemResponse> = emptyList(),
    val actions: List<AiAssistantWidgetActionResponse> = emptyList(),
    val followUpOptions: List<AiAssistantWidgetActionResponse> = emptyList()
)

data class AiAssistantWidgetItemResponse(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val meta: String? = null,
    val href: String? = null,
    val prompt: String? = null,
    val sourceType: String? = null,
    val sourceId: String? = null
)

data class AiAssistantWidgetActionResponse(
    val id: String,
    val label: String,
    val kind: String,
    val href: String? = null,
    val prompt: String? = null
)

data class AiAssistantDocumentHintResponse(
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

data class ChatHistoryResponse(
    val chatId: UUID,
    val messages: List<ChatHistoryMessageResponse>
)

data class ChatHistoryMessageResponse(
    val id: Long,
    val role: String,
    val content: String,
    val widget: AiAssistantWidgetResponse? = null,
    val createdAt: Instant
)

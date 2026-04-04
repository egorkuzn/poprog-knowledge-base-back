package com.example.poprogknowledgebaseback.adapters.inbound.web.assistant

import com.example.poprogknowledgebaseback.application.assistant.AiAssistantUseCase
import com.example.poprogknowledgebaseback.application.assistant.AssistantChatCommand
import com.example.poprogknowledgebaseback.application.assistant.AssistantChatResult
import com.example.poprogknowledgebaseback.application.assistant.ChatHistoryMessageResult
import com.example.poprogknowledgebaseback.application.assistant.ChatHistoryResult
import com.example.poprogknowledgebaseback.application.assistant.ChatHistoryUseCase
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assistant")
@Tag(name = "ИИ-ассистент", description = "Операции для общения с ИИ-ассистентом через GigaChat")
class AiAssistantController(
    private val aiAssistantUseCase: AiAssistantUseCase,
    private val chatHistoryUseCase: ChatHistoryUseCase
) {

    @PostMapping("/chat")
    @Operation(
        summary = "Отправить сообщение в чат с ИИ-ассистентом",
        description = "Принимает новые сообщения для диалога, при наличии chatId подмешивает сохраненную историю, отправляет запрос в GigaChat и возвращает ответ ассистента."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Ответ ИИ-ассистента успешно получен",
                content = [Content(schema = Schema(implementation = AiAssistantChatResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
            ApiResponse(responseCode = "404", description = "Диалог не найден"),
            ApiResponse(responseCode = "503", description = "Интеграция с GigaChat временно недоступна")
        ]
    )
    fun chat(@Valid @RequestBody request: AiAssistantChatRequest): AiAssistantChatResponse =
        aiAssistantUseCase.chat(
            AssistantChatCommand(
                chatId = request.chatId,
                messages = request.messages.map { it.toDomain() }
            )
        ).toDto()

    @GetMapping("/chats/{chatId}/messages")
    @Operation(
        summary = "Получить историю диалога с ИИ-ассистентом",
        description = "Возвращает сохраненную историю сообщений указанного диалога в хронологическом порядке."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "История диалога успешно получена",
                content = [Content(schema = Schema(implementation = ChatHistoryResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Диалог не найден")
        ]
    )
    fun getChatHistory(@PathVariable chatId: UUID): ChatHistoryResponse =
        chatHistoryUseCase.getHistory(chatId).toDto()

    private fun AiAssistantChatMessageRequest.toDomain() = AiChatMessage(
        role = AiChatMessageRole.valueOf(role.uppercase()),
        content = content
    )

    private fun AssistantChatResult.toDto() = AiAssistantChatResponse(
        chatId = chatId,
        content = content,
        model = model,
        finishReason = finishReason,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
    )

    private fun ChatHistoryResult.toDto() = ChatHistoryResponse(
        chatId = chatId,
        messages = messages.map { it.toDto() }
    )

    private fun ChatHistoryMessageResult.toDto() = ChatHistoryMessageResponse(
        id = id,
        role = role.name.lowercase(),
        content = content,
        createdAt = createdAt
    )
}

package com.example.poprogknowledgebaseback.adapters.outbound.assistant.gigachat

import com.fasterxml.jackson.annotation.JsonProperty
import com.example.poprogknowledgebaseback.config.GigaChatProperties
import com.example.poprogknowledgebaseback.domain.assistant.AiAssistantResponse
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.port.AiAssistantPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "true")
class GigaChatAiAssistantAdapter(
    private val gigaChatApiRestClient: RestClient,
    private val gigaChatTokenProvider: GigaChatTokenProvider,
    private val gigaChatProperties: GigaChatProperties
) : AiAssistantPort {

    override fun complete(messages: List<AiChatMessage>): AiAssistantResponse {
        require(messages.isNotEmpty()) { "At least one chat message is required" }

        val response = gigaChatApiRestClient.post()
            .uri("/api/v1/chat/completions")
            .headers { headers ->
                headers.setBearerAuth(gigaChatTokenProvider.getAccessToken())
            }
            .body(
                GigaChatChatCompletionRequest(
                    model = gigaChatProperties.model,
                    messages = messages.map { it.toPayload() },
                    stream = false
                )
            )
            .retrieve()
            .body(GigaChatChatCompletionResponse::class.java)
            ?: error("GigaChat returned an empty chat completion response")

        val firstChoice = response.choices.firstOrNull()
            ?: error("GigaChat returned no choices")

        return AiAssistantResponse(
            content = firstChoice.message.content,
            model = response.model,
            finishReason = firstChoice.finishReason,
            promptTokens = response.usage?.promptTokens,
            completionTokens = response.usage?.completionTokens,
            totalTokens = response.usage?.totalTokens
        )
    }

    private fun AiChatMessage.toPayload() = GigaChatChatMessagePayload(
        role = role.name.lowercase(),
        content = content
    )
}

data class GigaChatChatCompletionRequest(
    val model: String,
    val messages: List<GigaChatChatMessagePayload>,
    val stream: Boolean = false
)

data class GigaChatChatMessagePayload(
    val role: String,
    val content: String
)

data class GigaChatChatCompletionResponse(
    val choices: List<GigaChatChoice>,
    val created: Long? = null,
    val model: String,
    val usage: GigaChatUsage? = null,
    @JsonProperty("object")
    val objectType: String? = null
)

data class GigaChatChoice(
    val message: GigaChatChoiceMessage,
    val index: Int,
    @JsonProperty("finish_reason")
    val finishReason: String? = null
)

data class GigaChatChoiceMessage(
    val role: String,
    val content: String
)

data class GigaChatUsage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int? = null,
    @JsonProperty("completion_tokens")
    val completionTokens: Int? = null,
    @JsonProperty("total_tokens")
    val totalTokens: Int? = null
)

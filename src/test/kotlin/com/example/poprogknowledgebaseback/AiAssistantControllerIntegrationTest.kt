package com.example.poprogknowledgebaseback

import com.example.poprogknowledgebaseback.domain.assistant.AiAssistantResponse
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessageRole
import com.example.poprogknowledgebaseback.domain.assistant.port.AiAssistantPort
import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.ObjectMapper

@SpringBootTest(
    properties = [
        "app.gigachat.enabled=true"
    ]
)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AiAssistantControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var recordingAiAssistantPort: RecordingAiAssistantPort

    @Autowired
    private lateinit var publicationPersistencePort: PublicationPersistencePort

    @Test
    fun `should create chat and return persisted history`() {
        val responseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "messages": [
                        { "role": "system", "content": "Отвечай кратко" },
                        { "role": "user", "content": "Привет" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = objectMapper.readTree(responseBody)
        val chatId = UUID.fromString(response["chatId"].requiredText())
        assertEquals("Echo: Привет", response["content"].requiredText())

        val historyResponseBody = mockMvc.perform(get("/api/assistant/chats/{chatId}/messages", chatId))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val history = objectMapper.readTree(historyResponseBody)
        val messages = history["messages"]
        assertEquals(3, messages.size())
        assertEquals("system", messages[0]["role"].requiredText())
        assertEquals("user", messages[1]["role"].requiredText())
        assertEquals("assistant", messages[2]["role"].requiredText())
        assertEquals("Echo: Привет", messages[2]["content"].requiredText())

        assertEquals(1, recordingAiAssistantPort.recordedRequests.size)
        assertEquals(2, recordingAiAssistantPort.recordedRequests.first().size)
    }

    @Test
    fun `should continue existing chat with stored history`() {
        val createResponseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "messages": [
                        { "role": "user", "content": "Привет" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val chatId = UUID.fromString(objectMapper.readTree(createResponseBody)["chatId"].requiredText())

        val continueResponseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "chatId": "$chatId",
                      "messages": [
                        { "role": "user", "content": "Как дела?" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val continueResponse = objectMapper.readTree(continueResponseBody)
        assertEquals(chatId.toString(), continueResponse["chatId"].requiredText())
        assertEquals("Echo: Как дела?", continueResponse["content"].requiredText())

        val secondRequestMessages = recordingAiAssistantPort.recordedRequests.last()
        assertEquals(3, secondRequestMessages.size)
        assertEquals("Привет", secondRequestMessages[0].content)
        assertEquals(AiChatMessageRole.ASSISTANT, secondRequestMessages[1].role)
        assertEquals("Как дела?", secondRequestMessages[2].content)
    }

    @Test
    fun `should return not found for unknown chat`() {
        mockMvc.perform(get("/api/assistant/chats/{chatId}/messages", UUID.randomUUID()))
            .andExpect(status().isNotFound)

        mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "chatId": "${UUID.randomUUID()}",
                      "messages": [
                        { "role": "user", "content": "Привет" }
                      ]
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `should return publications widget without calling llm`() {
        publicationPersistencePort.save(
            Publication(
                id = null,
                year = 2025,
                authors = "Иванов И.И.",
                theme = "Reflex для безопасных систем управления",
                published = "Вестник Poprog",
                link = "https://example.org/reflex-paper.pdf"
            )
        )

        val responseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "messages": [
                        { "role": "user", "content": "Какие есть публикации?" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = objectMapper.readTree(responseBody)
        val chatId = UUID.fromString(response["chatId"].requiredText())
        assertEquals("widget", response["mode"].requiredText())
        assertEquals("publications_list", response["widget"]["widgetType"].requiredText())
        assertTrue(response["widget"]["items"].size() > 0)
        assertEquals(0, recordingAiAssistantPort.recordedRequests.size)

        val historyBody = mockMvc.perform(get("/api/assistant/chats/{chatId}/messages", chatId))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        val history = objectMapper.readTree(historyBody)
        assertEquals("publications_list", history["messages"][1]["widget"]["widgetType"].requiredText())
    }

    @Test
    fun `should return publications widget for fuzzy matched phrase`() {
        val responseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "messages": [
                        { "role": "user", "content": "какие есь публикацыи" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = objectMapper.readTree(responseBody)
        assertEquals("widget", response["mode"].requiredText())
        assertEquals("publications_list", response["widget"]["widgetType"].requiredText())
        assertEquals(0, recordingAiAssistantPort.recordedRequests.size)
    }

    @Test
    fun `should return branch widget for language question without calling llm`() {
        val responseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                    {
                      "messages": [
                        { "role": "user", "content": "Что у вас есть по Reflex?" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = objectMapper.readTree(responseBody)
        assertEquals("widget", response["mode"].requiredText())
        assertEquals("branch", response["widget"]["widgetType"].requiredText())
        assertTrue(response["widget"]["followUpOptions"].size() >= 3)
        assertEquals(0, recordingAiAssistantPort.recordedRequests.size)
    }

    @Test
    fun `should return project documentation widget for language documentation question without calling llm`() {
        val responseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "messages": [
                        { "role": "user", "content": "Где документация по Reflex?" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = objectMapper.readTree(responseBody)
        assertEquals("widget", response["mode"].requiredText())
        assertEquals("project_documentation", response["widget"]["widgetType"].requiredText())
        assertEquals("/projects/reflex/docs", response["widget"]["items"][0]["href"].requiredText())
        assertEquals(0, recordingAiAssistantPort.recordedRequests.size)
    }

    @Test
    fun `should keep llm path for document explanation`() {
        val responseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "messages": [
                        { "role": "user", "content": "Объясни, о чём эта публикация" }
                      ]
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val response = objectMapper.readTree(responseBody)
        assertEquals("text", response["mode"].requiredText())
        assertTrue(response["content"].requiredText().startsWith("Echo:"))
        assertEquals(1, recordingAiAssistantPort.recordedRequests.size)
    }

    companion object {
        @Container
        private val postgres = PostgreSQLContainer("postgres:18")

        @JvmStatic
        @DynamicPropertySource
        fun registerDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @TestConfiguration
    class StubAssistantConfig {
        @Bean
        @Primary
        fun recordingAiAssistantPort(): RecordingAiAssistantPort = RecordingAiAssistantPort()
    }

    class RecordingAiAssistantPort : AiAssistantPort {
        val recordedRequests: MutableList<List<AiChatMessage>> = CopyOnWriteArrayList()

        override fun complete(messages: List<AiChatMessage>): AiAssistantResponse {
            recordedRequests += messages.map { it.copy() }
            val lastUserMessage = messages.lastOrNull { it.role == AiChatMessageRole.USER }?.content ?: "empty"

            return AiAssistantResponse(
                content = "Echo: $lastUserMessage",
                model = "stub-gigachat",
                finishReason = "stop",
                promptTokens = messages.size * 2,
                completionTokens = 3,
                totalTokens = messages.size * 2 + 3
            )
        }
    }

    private fun tools.jackson.databind.JsonNode.requiredText(): String =
        toString().trim('"')
}

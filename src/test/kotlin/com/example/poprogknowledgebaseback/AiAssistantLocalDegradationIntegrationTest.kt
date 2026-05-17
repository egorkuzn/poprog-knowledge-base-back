package com.example.poprogknowledgebaseback

import com.example.poprogknowledgebaseback.domain.assistant.AiAssistantResponse
import com.example.poprogknowledgebaseback.domain.assistant.AiChatMessage
import com.example.poprogknowledgebaseback.domain.assistant.port.AiAssistantPort
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.springframework.web.client.ResourceAccessException
import tools.jackson.databind.ObjectMapper

@SpringBootTest(
    properties = [
        "app.gigachat.enabled=true"
    ]
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("local")
class AiAssistantLocalDegradationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return local fallback when gigachat request fails in local profile`() {
        val responseBody = mockMvc.perform(
            post("/api/assistant/chat")
                .contentType("application/json")
                .content(
                    """
                    {
                      "messages": [
                        { "role": "user", "content": "Тест деградации внешнего ИИ сервиса" }
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
        val chatId = UUID.fromString(response["chatId"].asText())
        assertEquals("local-fallback", response["model"].asText())
        assertTrue(response["content"].asText().contains("внешний ИИ-сервис временно недоступен"))

        val historyBody = mockMvc.perform(get("/api/assistant/chats/{chatId}/messages", chatId))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val history = objectMapper.readTree(historyBody)
        assertEquals(2, history["messages"].size())
        assertEquals("assistant", history["messages"][1]["role"].asText())
        assertTrue(history["messages"][1]["content"].asText().contains("внешний ИИ-сервис временно недоступен"))
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
    class FailingAssistantConfig {
        @Bean
        @Primary
        fun failingAiAssistantPort(): AiAssistantPort = object : AiAssistantPort {
            override fun complete(messages: List<AiChatMessage>): AiAssistantResponse {
                throw ResourceAccessException("Simulated TLS failure")
            }
        }
    }
}

package com.example.poprogknowledgebaseback

import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
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
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("local")
class AccountOwnershipIsolationIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should isolate favorites between different users`() {
        mockMvc.perform(
            post("/api/account/favorites")
                .header("subject", "user-a")
                .header("email", "a@example.com")
                .header("name", "User A")
                .header("roles", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "itemType": "publication",
                      "itemId": "pub-a",
                      "title": "Publication A",
                      "link": "/publications"
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/account/favorites")
                .header("subject", "user-b")
                .header("email", "b@example.com")
                .header("name", "User B")
                .header("roles", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "itemType": "student-work",
                      "itemId": "work-b",
                      "title": "Work B",
                      "link": "/works"
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isCreated)

        val userAFavorites = objectMapper.readTree(
            mockMvc.perform(
                get("/api/account/favorites")
                    .header("subject", "user-a")
                    .header("email", "a@example.com")
                    .header("name", "User A")
                    .header("roles", "USER")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )

        val userBFavorites = objectMapper.readTree(
            mockMvc.perform(
                get("/api/account/favorites")
                    .header("subject", "user-b")
                    .header("email", "b@example.com")
                    .header("name", "User B")
                    .header("roles", "USER")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )

        assertEquals(1, userAFavorites.size())
        assertEquals("pub-a", userAFavorites[0]["itemId"].asText())
        assertEquals(1, userBFavorites.size())
        assertEquals("work-b", userBFavorites[0]["itemId"].asText())
    }

    @Test
    fun `should isolate chats between different users`() {
        val userAChatId = UUID.fromString(
            objectMapper.readTree(
                mockMvc.perform(
                    post("/api/assistant/chat")
                        .header("subject", "chat-user-a")
                        .header("email", "chat-a@example.com")
                        .header("name", "Chat User A")
                        .header("roles", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "messages": [
                                { "role": "user", "content": "Привет от пользователя A" }
                              ]
                            }
                            """.trimIndent()
                        )
                )
                    .andExpect(status().isOk)
                    .andReturn()
                    .response
                    .contentAsString
            )["chatId"].asText()
        )

        mockMvc.perform(
            post("/api/assistant/chat")
                .header("subject", "chat-user-b")
                .header("email", "chat-b@example.com")
                .header("name", "Chat User B")
                .header("roles", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "messages": [
                        { "role": "user", "content": "Привет от пользователя B" }
                      ]
                    }
                    """.trimIndent()
                )
        ).andExpect(status().isOk)

        val userAChats = objectMapper.readTree(
            mockMvc.perform(
                get("/api/account/chats")
                    .header("subject", "chat-user-a")
                    .header("email", "chat-a@example.com")
                    .header("name", "Chat User A")
                    .header("roles", "USER")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )

        val userBChats = objectMapper.readTree(
            mockMvc.perform(
                get("/api/account/chats")
                    .header("subject", "chat-user-b")
                    .header("email", "chat-b@example.com")
                    .header("name", "Chat User B")
                    .header("roles", "USER")
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        )

        assertEquals(1, userAChats.size())
        assertEquals(userAChatId.toString(), userAChats[0]["chatId"].asText())
        assertTrue(userAChats[0]["lastMessagePreview"].asText().contains("пользователя A"))

        assertEquals(1, userBChats.size())
        assertTrue(userBChats[0]["lastMessagePreview"].asText().contains("пользователя B"))
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
}

package com.example.poprogknowledgebaseback

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SiteFeedbackControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should save site usefulness feedback`() {
        val payload =
            """
            {
              "helpful": true,
              "userName": "Egor Kuznecov",
              "userEmail": "egor@example.com",
              "source": "search-modal",
              "comment": "Хорошо работает поиск"
            }
            """.trimIndent()

        val responseBody = mockMvc.perform(
            post("/api/feedback/usefulness")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        assertTrue(root["id"].asLong() > 0)
    }

    @Test
    fun `should reject blank user name`() {
        val payload =
            """
            {
              "helpful": false,
              "userName": " ",
              "userEmail": "egor@example.com",
              "source": "search-modal"
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/feedback/usefulness")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isBadRequest)
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

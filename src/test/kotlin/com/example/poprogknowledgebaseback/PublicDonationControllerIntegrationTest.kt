package com.example.poprogknowledgebaseback

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
class PublicDonationControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create donation without auth headers`() {
        val payload =
            """
            {
              "amount": 1000,
              "currency": "RUB",
              "source": "public-page",
              "message": "Спасибо за проект",
              "returnUrl": "http://localhost:5173/donate",
              "userName": "Guest",
              "userEmail": "guest@example.com"
            }
            """.trimIndent()

        val response = mockMvc.perform(
            post("/api/donations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val json = objectMapper.readTree(response)
        assertEquals("RUB", json["currency"].asText())
        assertEquals("PENDING", json["status"].asText())
        assertTrue(json["confirmationUrl"].asText().isNotBlank())
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

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
class AccountDonationControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should create list and update donation status for current user`() {
        val createPayload =
            """
            {
              "amount": 1000.00,
              "currency": "RUB",
              "source": "support-page",
              "message": "Спасибо за проект",
              "returnUrl": "http://localhost:5173/donate/complete"
            }
            """.trimIndent()

        val createResponse = mockMvc.perform(
            post("/api/account/donations")
                .header("subject", "user-100")
                .header("email", "user100@example.com")
                .header("name", "User 100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val createJson = objectMapper.readTree(createResponse)
        val donationId = createJson["id"].asText()
        assertEquals("PENDING", createJson["status"].asText())
        assertTrue(createJson["confirmationUrl"].asText().contains(donationId))

        val listResponse = mockMvc.perform(
            get("/api/account/donations")
                .header("subject", "user-100")
                .header("email", "user100@example.com")
                .header("name", "User 100")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val listJson = objectMapper.readTree(listResponse)
        assertTrue(listJson.isArray)
        assertEquals(1, listJson.size())
        assertEquals(donationId, listJson[0]["id"].asText())

        val updatePayload =
            """
            {
              "status": "SUCCEEDED",
              "providerPaymentId": "yoopay-123"
            }
            """.trimIndent()

        val updateResponse = mockMvc.perform(
            post("/api/account/donations/$donationId/status")
                .header("subject", "user-100")
                .header("email", "user100@example.com")
                .header("name", "User 100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val updateJson = objectMapper.readTree(updateResponse)
        assertEquals("SUCCEEDED", updateJson["status"].asText())
        assertEquals("yoopay-123", updateJson["providerPaymentId"].asText())
        assertTrue(updateJson["paidAt"].asText().isNotBlank())
    }

    @Test
    fun `should reject donation calls without auth headers`() {
        mockMvc.perform(get("/api/account/donations"))
            .andExpect(status().isUnauthorized)
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

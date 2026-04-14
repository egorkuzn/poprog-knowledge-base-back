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
class AdminDonationReportingControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return donations kpi events and exports for admin`() {
        val createAccountPayload =
            """
            {
              "amount": 1200.00,
              "currency": "RUB",
              "source": "cabinet",
              "message": "Test donation",
              "returnUrl": "http://localhost:5173/donate/complete"
            }
            """.trimIndent()

        val createdDonationJson = objectMapper.readTree(
            mockMvc.perform(
                post("/api/account/donations")
                    .header("subject", "user-200")
                    .header("email", "user200@example.com")
                    .header("name", "User 200")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createAccountPayload)
            )
                .andExpect(status().isCreated)
                .andReturn()
                .response
                .contentAsString
        )
        val donationId = createdDonationJson["id"].asText()

        val updatePayload =
            """
            {
              "status": "SUCCEEDED",
              "providerPaymentId": "provider-200"
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/account/donations/$donationId/status")
                .header("subject", "user-200")
                .header("email", "user200@example.com")
                .header("name", "User 200")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
        )
            .andExpect(status().isOk)

        val createPublicPayload =
            """
            {
              "amount": 300.00,
              "currency": "RUB",
              "source": "landing",
              "message": "Public donation",
              "returnUrl": "http://localhost:5173/donate"
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/donations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPublicPayload)
        )
            .andExpect(status().isCreated)

        val kpiBody = mockMvc.perform(
            get("/api/admin/donations/kpi")
                .header("subject", "admin-1")
                .header("roles", "ADMIN")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val kpiJson = objectMapper.readTree(kpiBody)
        assertEquals(2, kpiJson["totalDonationsCount"].asInt())
        assertEquals(1, kpiJson["succeededDonationsCount"].asInt())
        assertEquals(1, kpiJson["pendingDonationsCount"].asInt())
        assertEquals("1500.00", kpiJson["totalAmount"].asText())

        val eventsBody = mockMvc.perform(
            get("/api/admin/donations/events")
                .header("subject", "admin-1")
                .header("roles", "ADMIN")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val eventsJson = objectMapper.readTree(eventsBody)
        assertEquals(2, eventsJson["totalCount"].asInt())
        assertEquals(0, eventsJson["page"].asInt())
        assertEquals(10, eventsJson["size"].asInt())
        assertTrue(eventsJson["items"].isArray)
        assertEquals(2, eventsJson["items"].size())

        val csvResponse = mockMvc.perform(
            get("/api/admin/donations/export.csv")
                .header("subject", "admin-1")
                .header("roles", "ADMIN")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response

        val csvPayload = csvResponse.contentAsString
        assertTrue(csvPayload.contains("eventType"))
        assertTrue(csvPayload.contains("DONATION_SUCCEEDED"))

        val pdfResponse = mockMvc.perform(
            get("/api/admin/donations/export.pdf")
                .header("subject", "admin-1")
                .header("roles", "ADMIN")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response

        val pdfPayload = pdfResponse.contentAsByteArray
        assertTrue(pdfPayload.isNotEmpty())
        val header = String(pdfPayload.copyOfRange(0, minOf(4, pdfPayload.size)))
        assertEquals("%PDF", header)
    }

    @Test
    fun `should reject donations reporting for non admin and anonymous users`() {
        mockMvc.perform(
            get("/api/admin/donations/kpi")
                .header("subject", "user-regular")
                .header("roles", "USER")
        )
            .andExpect(status().isForbidden)

        mockMvc.perform(get("/api/admin/donations/kpi"))
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

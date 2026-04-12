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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("local")
class AccountControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return unauthorized for account profile without auth headers`() {
        mockMvc.perform(get("/api/account/profile"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should get and update account profile using debug headers`() {
        val getResponse = mockMvc.perform(
            get("/api/account/profile")
                .header("subject", "user-123")
                .header("email", "tester@example.com")
                .header("name", "Test User")
                .header("roles", "USER")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val getJson = objectMapper.readTree(getResponse)
        assertEquals("user-123", getJson["subject"].asText())
        assertEquals("Test User", getJson["name"].asText())
        assertEquals("tester@example.com", getJson["email"].asText())

        val updatePayload =
            """
            {
              "name": "Updated User",
              "email": "updated@example.com"
            }
            """.trimIndent()

        val putResponse = mockMvc.perform(
            put("/api/account/profile")
                .header("subject", "user-123")
                .header("email", "tester@example.com")
                .header("name", "Test User")
                .header("roles", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val putJson = objectMapper.readTree(putResponse)
        assertEquals("Updated User", putJson["name"].asText())
        assertEquals("updated@example.com", putJson["email"].asText())
        assertTrue(putJson["roles"].isArray)
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

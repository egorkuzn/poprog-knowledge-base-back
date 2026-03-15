package com.example.poprogknowledgebaseback

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PublicationControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return publications grouped by year`() {
        val responseBody = mockMvc.perform(get("/api/publications/grouped"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        assertTrue(root.isArray)
        assertEquals(5, root.size())

        val firstGroup = root[0]
        assertEquals("2023", firstGroup["date"].asText())
        assertEquals(8, firstGroup["publications"].size())

        val lastGroup = root[root.size() - 1]
        assertEquals("2019", lastGroup["date"].asText())
        assertEquals(7, lastGroup["publications"].size())
    }

    @Test
    fun `should create update and delete publication`() {
        val createPayload =
            """
            {
              "year": 2024,
              "authors": "Test Author",
              "theme": "Test Theme",
              "published": "Test Published",
              "link": "https://example.org/test"
            }
            """.trimIndent()

        val createResponseBody = mockMvc.perform(
            post("/api/publications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val created = objectMapper.readTree(createResponseBody)
        val createdId = created["id"].asLong()
        assertEquals(2024, created["year"].asInt())

        val updatePayload =
            """
            {
              "year": 2025,
              "authors": "Updated Author",
              "theme": "Updated Theme",
              "published": "Updated Published",
              "link": ""
            }
            """.trimIndent()

        val updateResponseBody = mockMvc.perform(
            put("/api/publications/{id}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val updated = objectMapper.readTree(updateResponseBody)
        assertEquals(createdId, updated["id"].asLong())
        assertEquals(2025, updated["year"].asInt())
        assertEquals("Updated Theme", updated["theme"].asText())

        mockMvc.perform(delete("/api/publications/{id}", createdId))
            .andExpect(status().isNoContent)

        mockMvc.perform(put("/api/publications/{id}", createdId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updatePayload))
            .andExpect(status().isNotFound)
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

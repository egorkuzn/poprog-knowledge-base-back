package com.example.poprogknowledgebaseback

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.mock.web.MockMultipartFile
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
        assertTrue(root.size() >= 5)

        val groupsByDate = root.associateBy { it["date"].requiredText() }
        assertTrue(groupsByDate.containsKey("2023"))
        assertTrue(groupsByDate.containsKey("2019"))
        assertTrue(groupsByDate.getValue("2023")["publications"].size() >= 8)
        assertTrue(groupsByDate.getValue("2019")["publications"].size() >= 7)
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
        assertEquals("Updated Theme", updated["theme"].requiredText())

        mockMvc.perform(delete("/api/publications/{id}", createdId))
            .andExpect(status().isNoContent)

        mockMvc.perform(put("/api/publications/{id}", createdId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updatePayload))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should upload file and create publication`() {
        val metadata = MockMultipartFile(
            "metadata",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
            {
              "year": 2026,
              "authors": "Upload Author",
              "theme": "Upload Theme",
              "published": "Upload Published"
            }
            """.trimIndent().toByteArray()
        )
        val file = MockMultipartFile(
            "file",
            "publication.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".toByteArray()
        )

        val responseBody = mockMvc.perform(
            multipart("/api/publications/upload")
                .file(metadata)
                .file(file)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val created = objectMapper.readTree(responseBody)
        assertEquals(2026, created["year"].asInt())
        assertTrue(created["link"].requiredText().startsWith("/files/publications/"))
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

    private fun tools.jackson.databind.JsonNode.requiredText(): String =
        toString().trim('"')
}

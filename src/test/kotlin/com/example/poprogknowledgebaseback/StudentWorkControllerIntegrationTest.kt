package com.example.poprogknowledgebaseback

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
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
import org.springframework.mock.web.MockMultipartFile
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StudentWorkControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return student works grouped by project type`() {
        val responseBody = mockMvc.perform(get("/api/student-works/grouped"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        assertTrue(root.isArray)
        assertTrue(root.size() >= 7)

        val groupsByHash = root.associateBy { it["hash"].requiredText() }
        assertTrue(groupsByHash.containsKey("industrial-c"))
        assertTrue(groupsByHash.containsKey("requirements-engineering-and-edtl"))
        assertEquals("Язык IndustrialC", groupsByHash.getValue("industrial-c")["title"].requiredText())
        assertTrue(groupsByHash.getValue("industrial-c")["works"].size() >= 1)
        assertTrue(groupsByHash.getValue("requirements-engineering-and-edtl")["works"].size() >= 3)
    }

    @Test
    fun `should create update and delete student work`() {
        val createPayload =
            """
            {
              "projectTypeHash": "industrial-c",
              "authors": "Тестовый Автор",
              "theme": "Тестовая тема",
              "published": "Тестовая публикация"
            }
            """.trimIndent()

        val createResponseBody = mockMvc.perform(
            post("/api/student-works")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val created = objectMapper.readTree(createResponseBody)
        val createdId = created["id"].asLong()
        assertEquals("industrial-c", created["hash"].requiredText())

        val updatePayload =
            """
            {
              "projectTypeHash": "webide-and-extension-modules",
              "authors": "Обновленный Автор",
              "theme": "Обновленная тема",
              "published": "Обновленная публикация"
            }
            """.trimIndent()

        val updateResponseBody = mockMvc.perform(
            put("/api/student-works/{id}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val updated = objectMapper.readTree(updateResponseBody)
        assertEquals(createdId, updated["id"].asLong())
        assertEquals("webide-and-extension-modules", updated["hash"].requiredText())
        assertEquals("Обновленная тема", updated["theme"].requiredText())

        mockMvc.perform(delete("/api/student-works/{id}", createdId))
            .andExpect(status().isNoContent)

        mockMvc.perform(
            put("/api/student-works/{id}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `should upload file and create student work`() {
        val metadata = MockMultipartFile(
            "metadata",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
            {
              "projectTypeHash": "industrial-c",
              "authors": "Автор с файлом",
              "theme": "Тема с файлом",
              "published": "Публикация с файлом"
            }
            """.trimIndent().toByteArray()
        )
        val file = MockMultipartFile(
            "file",
            "student-work.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "pdf-content".toByteArray()
        )

        val responseBody = mockMvc.perform(
            multipart("/api/student-works/upload")
                .file(metadata)
                .file(file)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val created = objectMapper.readTree(responseBody)
        assertEquals("industrial-c", created["hash"].requiredText())
        assertTrue(created["documentLink"].requiredText().startsWith("/files/student-works/"))
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

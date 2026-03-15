package com.example.poprogknowledgebaseback

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
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
        assertEquals(7, root.size())

        val firstGroup = root[0]
        assertEquals("Язык IndustrialC", firstGroup["title"].asText())
        assertEquals("industrial-c", firstGroup["hash"].asText())
        assertEquals(1, firstGroup["works"].size())

        val secondGroup = root[1]
        assertEquals("requirements-engineering-and-edtl", secondGroup["hash"].asText())
        assertEquals(3, secondGroup["works"].size())
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
        assertEquals("industrial-c", created["hash"].asText())

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
        assertEquals("webide-and-extension-modules", updated["hash"].asText())
        assertEquals("Обновленная тема", updated["theme"].asText())

        mockMvc.perform(delete("/api/student-works/{id}", createdId))
            .andExpect(status().isNoContent)

        mockMvc.perform(
            put("/api/student-works/{id}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload)
        ).andExpect(status().isNotFound)
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

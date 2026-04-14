package com.example.poprogknowledgebaseback

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OpenApiContractIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should expose openapi document with main endpoints`() {
        val responseBody = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)

        assertEquals("3.1.0", root["openapi"].requiredText())
        assertEquals("POPROG Knowledge Base API", root["info"]["title"].requiredText())
        assertEquals("v1", root["info"]["version"].requiredText())

        val paths = root["paths"]
        assertTrue(paths.has("/api/publications/grouped"))
        assertTrue(paths.has("/api/publications/upload"))
        assertTrue(paths.has("/api/student-works/grouped"))
        assertTrue(paths.has("/api/student-works/upload"))
        assertTrue(paths.has("/api/search"))
        assertTrue(paths.has("/api/files/{path}"))
        assertTrue(paths.has("/api/feedback/usefulness"))
        assertTrue(paths.has("/api/account/profile"))
        assertTrue(paths.has("/api/account/chats"))
        assertTrue(paths.has("/api/account/favorites"))
        assertTrue(paths.has("/api/account/donations"))
        assertTrue(paths.has("/api/account/donations/export.csv"))
        assertTrue(paths.has("/api/account/donations/export.pdf"))
        assertTrue(paths.has("/api/admin/donations/kpi"))
        assertTrue(paths.has("/api/admin/donations/events"))
        assertTrue(paths.has("/api/admin/donations/export.csv"))
        assertTrue(paths.has("/api/admin/donations/export.pdf"))
        assertTrue(paths.has("/api/metrics/events"))
        assertTrue(paths.has("/api/metrics/reports/dau-wau"))
        assertTrue(paths.has("/api/metrics/reports/search-success"))
        assertTrue(paths.has("/api/metrics/reports/ctr"))
        assertTrue(paths.has("/api/market/apps"))
        assertTrue(paths.has("/api/market/categories"))
        assertTrue(paths.has("/api/donations"))
    }

    @Test
    fun `should mark admin mutation endpoints in openapi summaries`() {
        val responseBody = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        val paths = root["paths"]

        val adminOperations = listOf(
            "/api/publications" to "post",
            "/api/publications/upload" to "post",
            "/api/publications/{id}" to "put",
            "/api/publications/{id}" to "delete",
            "/api/student-works" to "post",
            "/api/student-works/upload" to "post",
            "/api/student-works/{id}" to "put",
            "/api/student-works/{id}" to "delete",
            "/api/projects/menu/sections" to "post",
            "/api/projects/menu/sections/{id}" to "put",
            "/api/projects/menu/sections/{id}" to "delete",
            "/api/projects/menu/items" to "post",
            "/api/projects/menu/items/{id}" to "put",
            "/api/projects/menu/items/{id}" to "delete",
            "/api/projects/menu/promos" to "post",
            "/api/projects/menu/promos/{id}" to "put",
            "/api/projects/menu/promos/{id}" to "delete",
            "/api/projects/menu/resources/upload" to "post",
            "/api/admin/donations/kpi" to "get",
            "/api/admin/donations/events" to "get",
            "/api/admin/donations/export.csv" to "get",
            "/api/admin/donations/export.pdf" to "get"
        )

        adminOperations.forEach { (path, method) ->
            val summary = paths[path][method]["summary"].requiredText()
            assertTrue(
                summary.startsWith("[ADMIN]"),
                "Operation $method $path must start with [ADMIN] in OpenAPI summary, but was: $summary"
            )
        }
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

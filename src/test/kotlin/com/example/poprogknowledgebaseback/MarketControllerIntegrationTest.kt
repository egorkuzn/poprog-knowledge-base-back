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
class MarketControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return categories and filtered apps from market`() {
        val categoriesResponse = mockMvc.perform(get("/api/market/categories"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val categoriesJson = objectMapper.readTree(categoriesResponse)
        assertTrue(categoriesJson["categories"].isArray)
        assertTrue(categoriesJson["categories"].size() > 0)

        val appsResponse = mockMvc.perform(get("/api/market/apps").param("q", "trace").param("category", "Аналитика"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val appsJson = objectMapper.readTree(appsResponse)
        assertEquals("trace", appsJson["query"].asText())
        assertEquals("Аналитика", appsJson["category"].asText())
        assertTrue(appsJson["total"].asInt() >= 1)
        assertTrue(appsJson["items"].isArray)
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

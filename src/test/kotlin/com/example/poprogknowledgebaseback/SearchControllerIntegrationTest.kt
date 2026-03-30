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
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper

@SpringBootTest(
    properties = [
        "app.search.enabled=true",
        "spring.autoconfigure.exclude="
    ]
)
@AutoConfigureMockMvc
@Testcontainers
class SearchControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return empty results for queries shorter than three characters`() {
        val responseBody = mockMvc.perform(get("/api/search").param("q", "ve"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        assertEquals("ve", root["query"].requiredText())
        assertEquals(0, root["total"].asInt())
        assertEquals(0, root["items"].size())
    }

    @Test
    fun `should search by prefix from three characters`() {
        val responseBody = mockMvc.perform(get("/api/search").param("q", "ver").param("limit", "10"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        assertEquals("ver", root["query"].requiredText())
        assertTrue(root["total"].asInt() > 0)
        assertTrue(
            root["items"].any {
                val haystack = listOf(
                    it["authors"].requiredText(),
                    it["theme"].requiredText(),
                    it["published"].requiredText()
                ).joinToString(" ").lowercase()
                haystack.contains("ver")
            }
        )
    }

    @Test
    fun `should search by substring inside indexed words`() {
        val responseBody = mockMvc.perform(get("/api/search").param("q", "cess").param("limit", "10"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        assertEquals("cess", root["query"].requiredText())
        assertTrue(root["total"].asInt() > 0)
        assertTrue(
            root["items"].any {
                val haystack = listOf(
                    it["authors"].requiredText(),
                    it["theme"].requiredText(),
                    it["published"].requiredText()
                ).joinToString(" ").lowercase()
                haystack.contains("cess")
            }
        )
    }

    companion object {
        @Container
        private val postgres = PostgreSQLContainer("postgres:18")

        @Container
        private val elasticsearch = GenericContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.2.5"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withExposedPorts(9200)

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.elasticsearch.uris") {
                "http://${elasticsearch.host}:${elasticsearch.getMappedPort(9200)}"
            }
        }
    }

    private fun tools.jackson.databind.JsonNode.requiredText(): String =
        toString().trim('"')
}

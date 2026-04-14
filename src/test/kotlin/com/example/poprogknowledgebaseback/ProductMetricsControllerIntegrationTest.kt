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
class ProductMetricsControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should ingest events and build dau wau ssr and ctr reports`() {
        saveEvent(
            """
            {
              "eventType": "page_view",
              "route": "/home",
              "referrer": "http://localhost:5173/",
              "sessionId": "session-a",
              "userKey": "user-a",
              "timestampClient": "2026-04-01T09:00:00Z",
              "payload": {}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "page_view",
              "route": "/docs",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-b",
              "userKey": "user-b",
              "timestampClient": "2026-04-01T10:00:00Z",
              "payload": {}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_query_submitted",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-a",
              "userKey": "user-a",
              "timestampClient": "2026-04-01T10:10:00Z",
              "payload": {"queryId":"q-1","query":"rust"}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_result_shown",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-a",
              "userKey": "user-a",
              "timestampClient": "2026-04-01T10:10:01Z",
              "payload": {"queryId":"q-1","publicationCount":2,"studentWorkCount":1}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_result_opened",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-a",
              "userKey": "user-a",
              "timestampClient": "2026-04-01T10:10:02Z",
              "payload": {"queryId":"q-1","sourceType":"publication"}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_result_click",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-a",
              "userKey": "user-a",
              "timestampClient": "2026-04-01T10:10:03Z",
              "payload": {"queryId":"q-1","sourceType":"publication"}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_query_submitted",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-b",
              "userKey": "user-b",
              "timestampClient": "2026-04-01T11:10:00Z",
              "payload": {"queryId":"q-2","query":"zig"}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_result_shown",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-b",
              "userKey": "user-b",
              "timestampClient": "2026-04-01T11:10:01Z",
              "payload": {"queryId":"q-2","publicationCount":0,"studentWorkCount":1}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_query_submitted",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-c",
              "userKey": "session-c",
              "timestampClient": "2026-04-02T08:00:00Z",
              "payload": {"queryId":"q-3","query":"post"}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_result_shown",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-c",
              "userKey": "session-c",
              "timestampClient": "2026-04-02T08:00:01Z",
              "payload": {"queryId":"q-3","publicationCount":0,"studentWorkCount":3}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_result_opened",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-c",
              "userKey": "session-c",
              "timestampClient": "2026-04-02T08:00:02Z",
              "payload": {"queryId":"q-3","sourceType":"student-work"}
            }
            """.trimIndent()
        )
        saveEvent(
            """
            {
              "eventType": "search_result_click",
              "route": "/home",
              "referrer": "http://localhost:5173/home",
              "sessionId": "session-c",
              "userKey": "session-c",
              "timestampClient": "2026-04-02T08:00:03Z",
              "payload": {"queryId":"q-3","sourceType":"student-work"}
            }
            """.trimIndent()
        )

        val dauWauBody = mockMvc.perform(get("/api/metrics/reports/dau-wau"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val dauWauJson = objectMapper.readTree(dauWauBody)
        assertEquals(
            "subject авторизованного пользователя или sessionId анонимной сессии",
            dauWauJson["uniqueUserDefinition"].asText()
        )
        assertEquals(2, dauWauJson["daily"][0]["uniqueUsers"].asInt())

        val searchSuccessBody = mockMvc.perform(get("/api/metrics/reports/search-success"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val searchSuccessJson = objectMapper.readTree(searchSuccessBody)
        assertEquals(2, searchSuccessJson["daily"].size())
        assertEquals(2, searchSuccessJson["daily"][0]["submittedCount"].asInt())
        assertEquals(1, searchSuccessJson["daily"][0]["successfulCount"].asInt())
        assertEquals("50.00", searchSuccessJson["daily"][0]["searchSuccessRatePercent"].asText())

        val ctrBody = mockMvc.perform(get("/api/metrics/reports/ctr"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val ctrJson = objectMapper.readTree(ctrBody)
        val ctrByType = ctrJson["items"].associateBy { it["sourceType"].asText() }
        assertEquals(2, ctrByType["publication"]?.get("impressions")?.asInt())
        assertEquals(1, ctrByType["publication"]?.get("clicks")?.asInt())
        assertEquals("50.00", ctrByType["publication"]?.get("clickThroughRatePercent")?.asText())
        assertEquals(5, ctrByType["student-work"]?.get("impressions")?.asInt())
        assertEquals(1, ctrByType["student-work"]?.get("clicks")?.asInt())
        assertEquals("20.00", ctrByType["student-work"]?.get("clickThroughRatePercent")?.asText())
    }

    private fun saveEvent(payload: String) {
        val responseBody = mockMvc.perform(
            post("/api/metrics/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isAccepted)
            .andReturn()
            .response
            .contentAsString

        val responseJson = objectMapper.readTree(responseBody)
        assertTrue(responseJson["id"].asLong() > 0)
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

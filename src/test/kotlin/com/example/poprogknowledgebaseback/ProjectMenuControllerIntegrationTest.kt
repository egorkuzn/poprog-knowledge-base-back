package com.example.poprogknowledgebaseback

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
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
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProjectMenuControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return project menu metadata`() {
        val responseBody = mockMvc.perform(get("/api/projects/menu"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val root = objectMapper.readTree(responseBody)
        assertEquals("Проекты", root["title"].requiredText())
        assertTrue(root["sections"].isArray)
        assertTrue(root["sections"].size() >= 5)

        val firstSection = root["sections"][0]
        assertEquals("programming-languages", firstSection["hash"].requiredText())
        assertTrue(firstSection["items"].size() >= 3)
        assertTrue(firstSection["promos"].size() >= 2)
    }

    @Test
    fun `should create update and delete project menu section`() {
        val createResponseBody = mockMvc.perform(
            post("/api/projects/menu/sections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "hash": "test-section",
                      "title": "Тестовая секция",
                      "description": "Описание тестовой секции",
                      "ctaTitle": "Перейти",
                      "ctaUrl": "/projects/test-section",
                      "sortOrder": 999
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val created = objectMapper.readTree(createResponseBody)
        val createdId = created["id"].asLong()
        assertEquals("test-section", created["hash"].requiredText())

        val updateResponseBody = mockMvc.perform(
            put("/api/projects/menu/sections/{id}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "hash": "test-section-updated",
                      "title": "Обновленная секция",
                      "description": "Новое описание",
                      "ctaTitle": "Подробнее",
                      "ctaUrl": "/projects/test-section-updated",
                      "sortOrder": 1000
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val updated = objectMapper.readTree(updateResponseBody)
        assertEquals("test-section-updated", updated["hash"].requiredText())
        assertEquals(1000, updated["sortOrder"].asInt())

        mockMvc.perform(delete("/api/projects/menu/sections/{id}", createdId))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should reject duplicate section hash`() {
        mockMvc.perform(
            post("/api/projects/menu/sections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "hash": "programming-languages",
                      "title": "Дубликат",
                      "description": "Нельзя создать секцию с тем же hash",
                      "ctaTitle": "Перейти",
                      "ctaUrl": "/projects/duplicate",
                      "sortOrder": 500
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should create update and delete project menu item`() {
        val sectionId = firstSectionId()

        val createResponseBody = mockMvc.perform(
            post("/api/projects/menu/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId": $sectionId,
                      "title": "Новый item",
                      "description": "Описание item",
                      "url": "/projects/new-item",
                      "imageUrl": "/files/projects-menu/new-item.png",
                      "highlighted": true,
                      "sortOrder": 55
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val created = objectMapper.readTree(createResponseBody)
        val createdId = created["id"].asLong()
        assertEquals("Новый item", created["title"].requiredText())

        val updateResponseBody = mockMvc.perform(
            put("/api/projects/menu/items/{id}", createdId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId": $sectionId,
                      "title": "Обновленный item",
                      "description": "Новое описание item",
                      "url": "/projects/new-item-updated",
                      "imageUrl": "/files/projects-menu/new-item-updated.png",
                      "highlighted": false,
                      "sortOrder": 56
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val updated = objectMapper.readTree(updateResponseBody)
        assertEquals("Обновленный item", updated["title"].requiredText())
        assertEquals(false, updated["highlighted"].asBoolean())

        mockMvc.perform(delete("/api/projects/menu/items/{id}", createdId))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should return 404 for missing promo on update`() {
        val sectionId = firstSectionId()

        mockMvc.perform(
            put("/api/projects/menu/promos/{id}", 999999)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId": $sectionId,
                      "title": "Обновленный промо",
                      "description": "Описание",
                      "url": "/projects/missing-promo",
                      "imageUrl": "/files/projects-menu/missing.png",
                      "sortOrder": 1
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should cascade delete items and promos with section`() {
        val createSectionResponse = mockMvc.perform(
            post("/api/projects/menu/sections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "hash": "cascade-section",
                      "title": "Каскадная секция",
                      "description": "Проверка каскадного удаления",
                      "ctaTitle": "Перейти",
                      "ctaUrl": "/projects/cascade-section",
                      "sortOrder": 700
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val sectionId = objectMapper.readTree(createSectionResponse)["id"].asLong()

        val createItemResponse = mockMvc.perform(
            post("/api/projects/menu/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId": $sectionId,
                      "title": "Каскадный item",
                      "description": "Описание",
                      "url": "/projects/cascade-item",
                      "highlighted": false,
                      "sortOrder": 1
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val itemId = objectMapper.readTree(createItemResponse)["id"].asLong()

        val createPromoResponse = mockMvc.perform(
            post("/api/projects/menu/promos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId": $sectionId,
                      "title": "Каскадный promo",
                      "description": "Описание",
                      "url": "/projects/cascade-promo",
                      "imageUrl": "/files/projects-menu/cascade-promo.png",
                      "sortOrder": 1
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val promoId = objectMapper.readTree(createPromoResponse)["id"].asLong()

        mockMvc.perform(delete("/api/projects/menu/sections/{id}", sectionId))
            .andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/projects/menu/items/{id}", itemId))
            .andExpect(status().isNotFound)
        mockMvc.perform(delete("/api/projects/menu/promos/{id}", promoId))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should upload project menu resource and create promo`() {
        val file = MockMultipartFile(
            "file",
            "promo.png",
            MediaType.IMAGE_PNG_VALUE,
            "png-content".toByteArray()
        )

        val uploadResponseBody = mockMvc.perform(
            multipart("/api/projects/menu/resources/upload")
                .file(file)
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val upload = objectMapper.readTree(uploadResponseBody)
        val resourceUrl = upload["resourceUrl"].requiredText()
        assertTrue(resourceUrl.startsWith("/files/projects-menu/"))

        val menuResponseBody = mockMvc.perform(get("/api/projects/menu"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        val sectionId = objectMapper.readTree(menuResponseBody)["sections"][0]["id"].asLong()

        val promoResponseBody = mockMvc.perform(
            post("/api/projects/menu/promos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sectionId": $sectionId,
                      "title": "Новый промо-блок",
                      "description": "Описание промо-блока",
                      "url": "/projects/promo",
                      "imageUrl": "$resourceUrl",
                      "sortOrder": 99
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val createdPromo = objectMapper.readTree(promoResponseBody)
        assertEquals(resourceUrl, createdPromo["imageUrl"].requiredText())
    }

    private fun firstSectionId(): Long {
        val menuResponseBody = mockMvc.perform(get("/api/projects/menu"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(menuResponseBody)["sections"][0]["id"].asLong()
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

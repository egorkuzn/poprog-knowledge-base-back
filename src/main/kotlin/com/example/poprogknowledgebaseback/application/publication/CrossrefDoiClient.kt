package com.example.poprogknowledgebaseback.application.publication

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import org.springframework.stereotype.Component

data class CrossrefWorkMeta(
    val containerTitle: String?,
    val publisher: String?,
    val volume: String?,
    val issue: String?,
    val page: String?,
    val articleNumber: String?,
    val year: Int?,
    val doi: String?
)

@Component
class CrossrefDoiClient {
    // Keep this client self-contained: we don't rely on a Spring-managed ObjectMapper bean.
    private val objectMapper: ObjectMapper = ObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun fetchWork(doi: String): CrossrefWorkMeta? {
        val normalized = doi.trim().removePrefix("https://doi.org/").removePrefix("http://doi.org/").removePrefix("doi.org/").trim()
        if (!normalized.startsWith("10.")) return null

        val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8)
        val uri = URI.create("https://api.crossref.org/works/$encoded")
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(12))
            .header("User-Agent", "PoprogKnowledgeBaseImporter/1.0 (+https://poprog.org)")
            .GET()
            .build()

        return runCatching {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) return@runCatching null
            val root = objectMapper.readTree(response.body())
            val message = root.path("message")
            CrossrefWorkMeta(
                containerTitle = message.firstText("container-title"),
                publisher = message.text("publisher"),
                volume = message.text("volume"),
                issue = message.text("issue"),
                page = message.text("page"),
                articleNumber = message.text("article-number"),
                year = message.extractIssuedYear(),
                doi = message.text("DOI") ?: normalized
            )
        }.getOrNull()
    }

    private fun JsonNode.text(field: String): String? =
        path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }

    private fun JsonNode.firstText(field: String): String? {
        val node = path(field)
        if (node.isMissingNode || node.isNull) return null
        if (node.isArray) {
            return node.firstOrNull()?.asText()?.takeIf { it.isNotBlank() }
        }
        return node.asText().takeIf { it.isNotBlank() }
    }

    private fun JsonNode.extractIssuedYear(): Int? {
        val issued = path("issued").path("date-parts")
        if (!issued.isArray || issued.isEmpty) return null
        val first = issued.firstOrNull() ?: return null
        val year = first.firstOrNull()?.asInt()
        return year?.takeIf { it in 1900..2100 }
    }
}

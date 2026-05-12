package com.example.poprogknowledgebaseback.application.importer

import com.example.poprogknowledgebaseback.application.lab19.Lab19NewsService
import com.example.poprogknowledgebaseback.application.lab19.UpsertLab19NewsItemCommand
import com.example.poprogknowledgebaseback.application.importer.Lab19MaterialClassifier
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.ArrayDeque
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class Lab19ImportService(
    private val lab19NewsService: Lab19NewsService,
    private val objectMapper: ObjectMapper
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun run(command: Lab19ImportCommand): Lab19ImportReport {
        val boundedMaxPages = command.maxPages.coerceIn(1, 100)
        val boundedMaxDepth = command.maxDepth.coerceIn(0, 4)
        val pages = crawlPages(startUrls = START_URLS, maxPages = boundedMaxPages, maxDepth = boundedMaxDepth)
        val discoveredLinks = pages.flatMap { page -> page.links }
            .distinctBy { it.url }
        val audits = discoveredLinks
            .parallelStream()
            .map { link -> auditLink(link) }
            .toList()
            .sortedBy { it.url }
        val pdfCandidates = audits
            .filter { it.status == "working" && it.looksLikePdf() }
            .map { it.toPdfCandidate() }
        val importedNewsItems: List<Lab19ImportedNewsItem>
        val importedPublications: List<Lab19ImportedPublication>
        val importedStudentWorks: List<Lab19ImportedStudentWork>
        if (command.dryRun) {
            importedNewsItems = emptyList()
            importedPublications = emptyList()
            importedStudentWorks = emptyList()
        } else {
            // Upsert everything into staging with classification, then import subsets to KB entities.
            pdfCandidates.forEach { candidate ->
                upsertStaging(candidate)
            }
            // Fix historic rows that were created before classifier existed or with different rules.
            lab19NewsService.reclassifyAll()
            importedPublications = importScientificPublications(
                importBaseUrl = command.importBaseUrl,
                candidates = pdfCandidates.filter { it.materialKind == MATERIAL_KIND_SCIENTIFIC_PUBLICATION }
            )
            importedStudentWorks = importStudentWorks(
                importBaseUrl = command.importBaseUrl,
                candidates = pdfCandidates.filter { it.materialKind == MATERIAL_KIND_STUDENT_WORK }
            )
            importedNewsItems = importNewsItems(pdfCandidates.filter { it.materialKind == MATERIAL_KIND_NEWS })
        }

        return Lab19ImportReport(
            startUrl = START_URLS.first(),
            dryRun = command.dryRun,
            visitedPages = pages.size,
            discoveredLinks = audits.size,
            workingLinks = audits.count { it.status == "working" },
            brokenLinks = audits.count { it.status == "broken" },
            ambiguousLinks = audits.count { it.status == "ambiguous" },
            pdfCandidates = pdfCandidates,
            importedNewsItems = importedNewsItems,
            importedPublications = importedPublications,
            importedStudentWorks = importedStudentWorks,
            linkAudit = audits
        )
    }

    private fun upsertStaging(candidate: Lab19PdfCandidate) {
        lab19NewsService.upsert(
            UpsertLab19NewsItemCommand(
                title = candidate.title,
                sourceUrl = candidate.url,
                sourcePage = candidate.sourcePage,
                year = candidate.year,
                contentType = candidate.contentType,
                materialKind = candidate.materialKind,
                status = candidate.status
            )
        )
    }

    private fun crawlPages(startUrls: List<String>, maxPages: Int, maxDepth: Int): List<CrawledPage> {
        val visited = linkedSetOf<String>()
        val result = mutableListOf<CrawledPage>()
        val queue = ArrayDeque<PageToVisit>()
        startUrls.forEach { url -> queue.add(PageToVisit(url, 0)) }

        while (queue.isNotEmpty() && result.size < maxPages) {
            val current = queue.removeFirst()
            if (!visited.add(current.url)) {
                continue
            }

            val document = fetchDocument(current.url) ?: continue
            val links = document.select("a[href]")
                .mapNotNull { anchor -> anchor.toInternalLink(current.url) }
                .distinctBy { it.url }
            result += CrawledPage(current.url, document.title().ifBlank { current.url }, links)

            if (current.depth < maxDepth) {
                links.asSequence()
                    .filter { it.url.isLikelyHtmlPage() }
                    .filterNot { visited.contains(it.url) }
                    .forEach { queue.add(PageToVisit(it.url, current.depth + 1)) }
            }
        }

        return result
    }

    private fun fetchDocument(url: String): Document? =
        runCatching {
            Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(12_000)
                .get()
        }.getOrNull()

    private fun auditLink(link: DiscoveredLink): Lab19LinkAuditItem {
        if (isTelegramUrl(link.url)) {
            return Lab19LinkAuditItem(
                url = link.url,
                sourcePage = link.sourcePage,
                label = link.label,
                status = "working",
                error = "Reachability check skipped for Telegram host"
            )
        }

        val request = HttpRequest.newBuilder(URI.create(link.url))
            .timeout(LINK_AUDIT_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build()
        return runCatching {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            val statusCode = response.statusCode()
            val status = when {
                statusCode in 200..399 -> "working"
                statusCode == 404 || statusCode == 410 -> "broken"
                statusCode == 405 -> auditByGet(link)
                else -> "ambiguous"
            }
            if (status == "working" || status == "broken" || status == "ambiguous") {
                Lab19LinkAuditItem(
                    url = link.url,
                    sourcePage = link.sourcePage,
                    label = link.label,
                    status = status,
                    httpStatus = statusCode,
                    contentType = response.headers().firstValue("content-type").orElse(null)
                )
            } else {
                error("Unexpected status marker: $status")
            }
        }.getOrElse { error ->
            Lab19LinkAuditItem(
                url = link.url,
                sourcePage = link.sourcePage,
                label = link.label,
                status = "ambiguous",
                error = error.message
            )
        }
    }

    private fun auditByGet(link: DiscoveredLink): String {
        val request = HttpRequest.newBuilder(URI.create(link.url))
            .timeout(LINK_AUDIT_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build()
        return runCatching {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            when (response.statusCode()) {
                in 200..399 -> "working"
                404, 410 -> "broken"
                else -> "ambiguous"
            }
        }.getOrDefault("ambiguous")
    }

    private fun importScientificPublications(importBaseUrl: String, candidates: List<Lab19PdfCandidate>): List<Lab19ImportedPublication> =
        candidates.map { candidate ->
            runCatching {
                val pdfBytes = downloadPdf(candidate.url)
                val pdfMeta = extractPdfMeta(pdfBytes, candidate.title)
                val metadata = PublicationUploadPayload(
                    year = candidate.year ?: DEFAULT_YEAR,
                    authors = pdfMeta.authors ?: DEFAULT_AUTHORS,
                    theme = candidate.title,
                    published = pdfMeta.published ?: buildPublishedFromSource(candidate.sourcePage)
                )
                val responseJson = postMultipart(
                    url = "$importBaseUrl/publications/upload",
                    metadataJson = objectMapper.writeValueAsBytes(metadata),
                    fileBytes = pdfBytes,
                    fileName = guessFileName(candidate.url)
                )
                val parsed = objectMapper.readValue(responseJson, PublicationUploadResponse::class.java)
                lab19NewsService.attachKbPublicationId(candidate.url, parsed.id)
                Lab19ImportedPublication(
                    sourceUrl = candidate.url,
                    publicationId = parsed.id,
                    title = candidate.title,
                    status = "imported"
                )
            }.getOrElse { error ->
                Lab19ImportedPublication(
                    sourceUrl = candidate.url,
                    publicationId = null,
                    title = candidate.title,
                    status = "failed",
                    error = error.message
                )
            }
        }

    private fun importStudentWorks(importBaseUrl: String, candidates: List<Lab19PdfCandidate>): List<Lab19ImportedStudentWork> =
        candidates.map { candidate ->
            runCatching {
                val pdfBytes = downloadPdf(candidate.url)
                val pdfMeta = extractPdfMeta(pdfBytes, candidate.title)
                val metadata = StudentWorkUploadPayload(
                    projectTypeHash = DEFAULT_STUDENT_WORK_PROJECT_HASH,
                    authors = pdfMeta.authors ?: DEFAULT_AUTHORS,
                    theme = candidate.title,
                    published = pdfMeta.published ?: buildPublishedFromSource(candidate.sourcePage)
                )
                val responseJson = postMultipart(
                    url = "$importBaseUrl/student-works/upload",
                    metadataJson = objectMapper.writeValueAsBytes(metadata),
                    fileBytes = pdfBytes,
                    fileName = guessFileName(candidate.url)
                )
                val parsed = objectMapper.readValue(responseJson, StudentWorkUploadResponse::class.java)
                lab19NewsService.attachKbStudentWorkId(candidate.url, parsed.id)
                Lab19ImportedStudentWork(
                    sourceUrl = candidate.url,
                    workId = parsed.id,
                    title = candidate.title,
                    status = "imported"
                )
            }.getOrElse { error ->
                Lab19ImportedStudentWork(
                    sourceUrl = candidate.url,
                    workId = null,
                    title = candidate.title,
                    status = "failed",
                    error = error.message
                )
            }
        }

    private fun importNewsItems(candidates: List<Lab19PdfCandidate>): List<Lab19ImportedNewsItem> =
        candidates.map { candidate ->
            runCatching {
                val saved = lab19NewsService.upsert(
                    UpsertLab19NewsItemCommand(
                        title = candidate.title,
                        sourceUrl = candidate.url,
                        sourcePage = candidate.sourcePage,
                        year = candidate.year,
                        contentType = candidate.contentType,
                        materialKind = MATERIAL_KIND_NEWS,
                        status = candidate.status
                    )
                )

                Lab19ImportedNewsItem(
                    sourceUrl = candidate.url,
                    newsItemId = saved.id,
                    title = candidate.title,
                    status = "imported"
                )
            }.getOrElse { error ->
                Lab19ImportedNewsItem(
                    sourceUrl = candidate.url,
                    newsItemId = null,
                    title = candidate.title,
                    status = "failed",
                    error = error.message
                )
            }
        }

    private fun downloadPdf(url: String): ByteArray {
        val request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(20))
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            error("Failed to download PDF: httpStatus=${response.statusCode()}")
        }
        return response.body()
    }

    // Exposed only for internal sync service (kept package-visible by Kotlin default).
    internal fun downloadPdfForSync(url: String): ByteArray = downloadPdf(url)

    private fun postMultipart(
        url: String,
        metadataJson: ByteArray,
        fileBytes: ByteArray,
        fileName: String
    ): ByteArray {
        val boundary = "----poprog-${UUID.randomUUID()}"
        val body = MultipartBodyBuilder(boundary)
            .addJsonPart(name = "metadata", jsonBytes = metadataJson)
            .addFilePart(name = "file", fileName = fileName, contentType = "application/pdf", bytes = fileBytes)
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .header("Accept", "application/json")
            .header(DEV_HEADER_SUBJECT, "lab19-importer")
            .header(DEV_HEADER_ROLES, "ADMIN")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            val preview = runCatching { String(response.body()).take(400) }.getOrDefault("")
            error("Upload failed: httpStatus=${response.statusCode()} body=$preview")
        }
        return response.body()
    }

    private fun guessFileName(url: String): String =
        url.substringBefore('?').substringAfterLast('/').ifBlank { "document.pdf" }

    private fun buildPublishedFromSource(sourcePage: String): String {
        val normalized = sourcePage.trim()
        return if (normalized.isBlank()) {
            "ИАиЭ СО РАН, лаборатория 19"
        } else {
            "ИАиЭ СО РАН, лаборатория 19; источник: $normalized"
        }
    }

    internal data class PdfMeta(
        val authors: String?,
        val published: String?
    )

    private fun extractPdfMeta(pdfBytes: ByteArray, titleHint: String): PdfMeta {
        return runCatching {
            PDDocument.load(pdfBytes).use { document ->
                val stripper = PDFTextStripper().apply {
                    // Keep this bounded: we only need the first few pages for metadata extraction.
                    startPage = 1
                    endPage = minOf(3, document.numberOfPages)
                }
                val raw = stripper.getText(document)
                val lines = raw
                    .split('\n')
                    .map { it.replace(Regex("\\s+"), " ").trim() }
                    .filter { it.isNotBlank() }

                val authors = extractAuthors(lines)
                val published = extractPublished(lines, titleHint)
                PdfMeta(authors = authors, published = published)
            }
        }.getOrElse { PdfMeta(authors = null, published = null) }
    }

    internal fun extractPdfMetaForSync(pdfBytes: ByteArray, titleHint: String): PdfMeta =
        extractPdfMeta(pdfBytes, titleHint)

    private fun extractAuthors(lines: List<String>): String? {
        // Prefer explicit "Авторы:" markers when present.
        lines.firstOrNull { it.startsWith("Авторы:", ignoreCase = true) }?.let { line ->
            return line.substringAfter(':').trim().takeIf { it.isNotBlank() }
        }
        lines.firstOrNull { it.startsWith("Authors:", ignoreCase = true) }?.let { line ->
            return line.substringAfter(':').trim().takeIf { it.isNotBlank() }
        }

        // Fallback: detect a line with 2+ "Surname I.O." patterns.
        val nameRegex = Regex("([A-ZА-ЯЁ][A-Za-zА-Яа-яЁё\\-]+\\s+[A-ZА-ЯЁ]\\.?\\s*[A-ZА-ЯЁ]\\.?)")
        return lines.asSequence()
            .map { line -> line to nameRegex.findAll(line).map { it.value }.toList() }
            .firstOrNull { (_, matches) -> matches.size >= 2 }
            ?.second
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractPublished(lines: List<String>, titleHint: String): String? {
        val normalizedHint = titleHint.lowercase()
        val hintTokens = normalizedHint
            .split(Regex("[^a-zа-яё0-9]+"))
            .filter { it.length >= 4 }
            .take(6)

        // Prefer citation-like lines with "//" and/or "DOI".
        val candidates = lines.filter { it.contains("//") || it.contains("DOI", ignoreCase = true) || it.contains("10.", ignoreCase = true) }
        val best = candidates
            .sortedByDescending { line -> hintTokens.count { token -> line.lowercase().contains(token) } }
            .firstOrNull()
            ?.takeIf { it.length >= 20 }

        return best
    }

    private fun Element.toInternalLink(sourcePage: String): DiscoveredLink? {
        val rawHref = attr("href").trim()
        if (rawHref.isBlank() || rawHref.startsWith("#") || rawHref.startsWith("mailto:") || rawHref.startsWith("tel:")) {
            return null
        }
        val resolved = runCatching { URI(sourcePage).resolve(rawHref).normalize() }.getOrNull() ?: return null
        if (resolved.scheme !in setOf("http", "https") || resolved.host?.lowercase() != HOST) {
            return null
        }
        val withoutFragment = URI(resolved.scheme, resolved.authority, resolved.path, resolved.query, null).toString()
        return DiscoveredLink(
            url = withoutFragment,
            sourcePage = sourcePage,
            label = text().replace(Regex("\\s+"), " ").trim().ifBlank { rawHref }
        )
    }

    private fun Lab19LinkAuditItem.looksLikePdf(): Boolean =
        url.substringBefore('?').lowercase().endsWith(".pdf") ||
            contentType?.lowercase()?.contains("application/pdf") == true

    private fun Lab19LinkAuditItem.toPdfCandidate(): Lab19PdfCandidate =
        Lab19PdfCandidate(
            url = url,
            sourcePage = sourcePage,
            title = label.ifBlank { url.substringAfterLast('/') },
            year = Regex("(19|20)\\d{2}").find(label)?.value?.toIntOrNull(),
            status = status,
            contentType = contentType,
            materialKind = classifyMaterialKind(sourcePage, label, url)
        )

    private fun String.isLikelyHtmlPage(): Boolean {
        val path = substringBefore('?').lowercase()
        val fileName = path.substringAfterLast('/')
        return fileName.isBlank() || !fileName.contains('.') || fileName.endsWith(".html") || fileName.endsWith(".php")
    }

    private data class PageToVisit(val url: String, val depth: Int)
    private data class CrawledPage(val url: String, val title: String, val links: List<DiscoveredLink>)
    private data class DiscoveredLink(val url: String, val sourcePage: String, val label: String)

    private fun classifyMaterialKind(sourcePage: String, title: String, url: String): String {
        return Lab19MaterialClassifier.classify(
            sourcePage = sourcePage,
            title = title,
            url = url
        )
    }

    companion object {
        private val START_URLS = listOf(
            "https://www.iae.nsk.su/ru/laboratory-sites/lab-19",
            "https://www.iae.nsk.su/ru/meaningful-results"
        )
        private const val HOST = "www.iae.nsk.su"
        private const val USER_AGENT = "PoprogKnowledgeBaseImporter/1.0 (+https://poprog.org)"
        private val LINK_AUDIT_TIMEOUT: Duration = Duration.ofSeconds(4)

        private const val DEV_HEADER_SUBJECT = "subject"
        private const val DEV_HEADER_ROLES = "roles"

        private const val DEFAULT_AUTHORS = "Лаборатория 19 ИАиЭ СО РАН"
        private const val DEFAULT_STUDENT_WORK_PROJECT_HASH = "verification-and-debugging-of-process-oriented-programs"
        private const val DEFAULT_YEAR = 2024

        private const val MATERIAL_KIND_SCIENTIFIC_PUBLICATION = Lab19MaterialClassifier.SCIENTIFIC_PUBLICATION
        private const val MATERIAL_KIND_STUDENT_WORK = Lab19MaterialClassifier.STUDENT_WORK
        private const val MATERIAL_KIND_NEWS = Lab19MaterialClassifier.NEWS
        private const val MATERIAL_KIND_IGNORE = Lab19MaterialClassifier.IGNORE
    }
}

private data class PublicationUploadPayload(
    val year: Int,
    val authors: String,
    val theme: String,
    val published: String
)

private data class StudentWorkUploadPayload(
    val projectTypeHash: String,
    val authors: String,
    val theme: String,
    val published: String
)

private data class PublicationUploadResponse(
    val id: Long
)

private data class StudentWorkUploadResponse(
    val id: Long
)

internal fun isTelegramUrl(url: String): Boolean {
    val host = runCatching { URI.create(url).host?.lowercase() }.getOrNull() ?: return false
    return host == "t.me" ||
        host.endsWith(".t.me") ||
        host == "telegram.me" ||
        host.endsWith(".telegram.me") ||
        host == "telegram.org" ||
        host.endsWith(".telegram.org")
}

private class MultipartBodyBuilder(private val boundary: String) {
    private val parts = ArrayList<ByteArray>()

    fun addJsonPart(name: String, jsonBytes: ByteArray): MultipartBodyBuilder {
        val header = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
        val footer = "\r\n".toByteArray(Charsets.UTF_8)
        parts += header
        parts += jsonBytes
        parts += footer
        return this
    }

    fun addFilePart(name: String, fileName: String, contentType: String, bytes: ByteArray): MultipartBodyBuilder {
        val header = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"").append(name).append("\"; filename=\"").append(fileName).append("\"\r\n")
            append("Content-Type: ").append(contentType).append("\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
        val footer = "\r\n".toByteArray(Charsets.UTF_8)
        parts += header
        parts += bytes
        parts += footer
        return this
    }

    fun build(): ByteArray {
        val closing = ("--$boundary--\r\n").toByteArray(Charsets.UTF_8)
        val totalSize = parts.sumOf { it.size } + closing.size
        val out = ByteArray(totalSize)
        var pos = 0
        for (part in parts) {
            part.copyInto(out, destinationOffset = pos)
            pos += part.size
        }
        closing.copyInto(out, destinationOffset = pos)
        return out
    }
}

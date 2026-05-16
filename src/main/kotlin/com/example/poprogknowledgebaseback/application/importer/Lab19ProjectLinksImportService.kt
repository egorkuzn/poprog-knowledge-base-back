package com.example.poprogknowledgebaseback.application.importer

import com.example.poprogknowledgebaseback.application.projectmenu.ProjectMenuUseCase
import com.example.poprogknowledgebaseback.application.projectmenu.UpsertProjectMenuItemCommand
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.ArrayDeque
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Service

data class Lab19ProjectLinksImportCommand(
    val dryRun: Boolean = true,
    val maxPages: Int = 30,
    val maxDepth: Int = 2,
    val sectionHash: String = "programming-languages"
)

data class Lab19ProjectLinkCandidate(
    val url: String,
    val sourcePage: String,
    val label: String,
    val status: String,
    val httpStatus: Int? = null
)

data class Lab19ProjectLinksImportReport(
    val startUrls: List<String>,
    val dryRun: Boolean,
    val visitedPages: Int,
    val discoveredLinks: Int,
    val workingLinks: Int,
    val addedItems: Int,
    val candidates: List<Lab19ProjectLinkCandidate>,
    val errors: List<String>
)

@Service
class Lab19ProjectLinksImportService(
    private val projectMenuUseCase: ProjectMenuUseCase
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun run(command: Lab19ProjectLinksImportCommand): Lab19ProjectLinksImportReport {
        val maxPages = command.maxPages.coerceIn(1, 100)
        val maxDepth = command.maxDepth.coerceIn(0, 4)

        val pages = crawlPages(startUrls = START_URLS, maxPages = maxPages, maxDepth = maxDepth)
        val discoveredLinks = pages.flatMap { it.links }.distinctBy { it.url }

        val audited = discoveredLinks
            .filter { it.url.isLikelyHtmlPage() }
            .filterNot { it.url.lowercase().endsWith(".pdf") }
            .filter { it.looksRelevantToPoprog() }
            .map { it.audit() }

        val working = audited.filter { it.status == "working" }

        val errors = mutableListOf<String>()
        var addedItems = 0

        val menu = projectMenuUseCase.getMenu()
        val section = menu.sections.firstOrNull { it.hash == command.sectionHash }
        if (section == null) {
            errors += "Project menu section hash not found: ${command.sectionHash}"
        } else if (!command.dryRun) {
            val existingUrls = section.items.map { it.url }.toSet()
            val startingSortOrder = (section.items.maxOfOrNull { it.sortOrder } ?: 0) + 1
            var nextSortOrder = startingSortOrder

            working.forEach { candidate ->
                if (candidate.url in existingUrls) {
                    return@forEach
                }
                runCatching {
                    projectMenuUseCase.createItem(
                        UpsertProjectMenuItemCommand(
                            sectionId = section.id,
                            title = candidate.label.take(180),
                            description = "Ссылка с сайта лаборатории 19 ИАиЭ СО РАН (проверена на работоспособность).",
                            url = candidate.url,
                            imageUrl = null,
                            highlighted = false,
                            sortOrder = nextSortOrder++
                        )
                    )
                    addedItems++
                }.onFailure { ex ->
                    errors += "Failed to add item for url=${candidate.url}: ${ex.message}"
                }
            }
        }

        return Lab19ProjectLinksImportReport(
            startUrls = START_URLS,
            dryRun = command.dryRun,
            visitedPages = pages.size,
            discoveredLinks = discoveredLinks.size,
            workingLinks = working.size,
            addedItems = addedItems,
            candidates = audited.sortedBy { it.url },
            errors = errors
        )
    }

    private fun crawlPages(startUrls: List<String>, maxPages: Int, maxDepth: Int): List<CrawledPage> {
        val visited = linkedSetOf<String>()
        val result = mutableListOf<CrawledPage>()
        val queue = ArrayDeque<PageToVisit>()
        startUrls.forEach { url -> queue.add(PageToVisit(url, 0)) }

        while (queue.isNotEmpty() && result.size < maxPages) {
            val current = queue.removeFirst()
            if (!visited.add(current.url)) continue

            val document = fetchDocument(current.url) ?: continue
            val links = document.select("a[href]")
                .mapNotNull { anchor -> anchor.toResolvedLink(current.url) }
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

    private fun DiscoveredLink.audit(): Lab19ProjectLinkCandidate {
        val request = HttpRequest.newBuilder(URI.create(url))
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
                statusCode == 405 -> auditByGet()
                else -> "ambiguous"
            }
            Lab19ProjectLinkCandidate(
                url = url,
                sourcePage = sourcePage,
                label = label,
                status = status,
                httpStatus = statusCode
            )
        }.getOrElse { ex ->
            Lab19ProjectLinkCandidate(
                url = url,
                sourcePage = sourcePage,
                label = label,
                status = "ambiguous",
                httpStatus = null
            )
        }
    }

    private fun DiscoveredLink.auditByGet(): String {
        val request = HttpRequest.newBuilder(URI.create(url))
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

    private fun Element.toResolvedLink(sourcePage: String): DiscoveredLink? {
        val rawHref = attr("href").trim()
        if (rawHref.isBlank() || rawHref.startsWith("#") || rawHref.startsWith("mailto:") || rawHref.startsWith("tel:")) {
            return null
        }
        val resolved = runCatching { URI(sourcePage).resolve(rawHref).normalize() }.getOrNull() ?: return null
        if (resolved.scheme !in setOf("http", "https")) {
            return null
        }
        val withoutFragment = URI(resolved.scheme, resolved.authority, resolved.path, resolved.query, null).toString()
        return DiscoveredLink(
            url = withoutFragment,
            sourcePage = sourcePage,
            label = text().replace(Regex("\\s+"), " ").trim().ifBlank { rawHref }
        )
    }

    private fun String.isLikelyHtmlPage(): Boolean {
        val path = substringBefore('?').lowercase()
        val fileName = path.substringAfterLast('/')
        return fileName.isBlank() || !fileName.contains('.') || fileName.endsWith(".html") || fileName.endsWith(".php")
    }

    private fun DiscoveredLink.looksRelevantToPoprog(): Boolean {
        val text = "$label $url"
        return RELEVANT_PATTERNS.any { pattern -> pattern.containsMatchIn(text) }
    }

    private data class PageToVisit(val url: String, val depth: Int)
    private data class CrawledPage(val url: String, val title: String, val links: List<DiscoveredLink>)
    private data class DiscoveredLink(val url: String, val sourcePage: String, val label: String)

    companion object {
        private val START_URLS = listOf(
            "https://www.iae.nsk.su/ru/laboratory-sites/lab-19",
            "https://www.iae.nsk.su/ru/meaningful-results"
        )
        private const val USER_AGENT = "PoprogKnowledgeBaseImporter/1.0 (+https://poprog.org)"
        private val LINK_AUDIT_TIMEOUT: Duration = Duration.ofSeconds(4)

        // Only languages related to poprog (per your constraint).
        private val RELEVANT_PATTERNS = listOf(
            Regex("\\bpost2st\\b", RegexOption.IGNORE_CASE),
            Regex("\\bReflex\\b", RegexOption.IGNORE_CASE),
            Regex("\\bIndustrialC\\b", RegexOption.IGNORE_CASE),
            Regex("\\bIndustrial-C\\b", RegexOption.IGNORE_CASE),
            // Avoid false positives like "post-graduate": match poST explicitly.
            Regex("\\bpoST\\b"),
            Regex("\\bPoST\\b")
        )
    }
}

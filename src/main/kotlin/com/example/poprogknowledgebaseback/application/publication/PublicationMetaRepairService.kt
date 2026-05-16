package com.example.poprogknowledgebaseback.application.publication

import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class PublicationRepairReport(
    val scanned: Int,
    val repaired: Int,
    val deleted: Int,
    val invalidIds: List<Long>
)

@Service
class PublicationMetaRepairService(
    private val publicationPersistencePort: PublicationPersistencePort,
    private val crossrefDoiClient: CrossrefDoiClient
) {
    @Transactional
    fun repairAndPrune(dryRun: Boolean = true): PublicationRepairReport {
        val publications = publicationPersistencePort.findAllOrderByYearDescIdAsc()
        var repaired = 0
        val invalidIds = mutableListOf<Long>()

        publications.forEach { pub ->
            val id = pub.id ?: return@forEach
            val repairedPub = repairOne(pub)
            if (repairedPub != pub) {
                repaired++
                if (!dryRun) {
                    publicationPersistencePort.save(repairedPub)
                }
            }

            if (!isValid(repairedPub)) {
                invalidIds += id
            }
        }

        if (!dryRun) {
            invalidIds.forEach { publicationPersistencePort.deleteById(it) }
        }

        return PublicationRepairReport(
            scanned = publications.size,
            repaired = repaired,
            deleted = if (dryRun) 0 else invalidIds.size,
            invalidIds = invalidIds
        )
    }

    private fun repairOne(pub: com.example.poprogknowledgebaseback.domain.publication.Publication): com.example.poprogknowledgebaseback.domain.publication.Publication {
        val pdf = pub.pdfText?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        var authors = pub.authors
        var published = pub.published
        var year = pub.year

        // Fix "authors" when it contains obvious org garbage or is too short.
        if (authors.isBlank() || looksLikeOrgAuthors(authors)) {
            extractAuthorsFromPdfText(pdf)?.let { extracted ->
                authors = extracted
            }
        }

        // Fix "published" when it is missing or looks like a placeholder.
        if (published.isBlank() || looksLikePlaceholderPublished(published)) {
            extractPublishedFromPdfText(pdf, pub.theme)?.let { extracted ->
                published = extracted
            }
        } else {
            // Trim narrative tails if they were accidentally appended.
            published = published.replace(Regex("(?is)\\s+(Разработан|Разработана|Разработано|Разработаны|Впервые|Продемонстр\\w*|В работе|В статье|Показано|Рассмотрено|Описание|Для решения|Получен\\w*|Достигнут\\w*).*$"), "")
                .trim()
        }

        // If the string looks like "doi.org/..." - resolve via Crossref and replace with a full venue reference.
        extractDoi(published)?.let { doi ->
            crossrefDoiClient.fetchWork(doi)?.let { meta ->
                val venue = formatVenue(meta)
                if (venue.isNotBlank()) {
                    published = venue
                }
                meta.year?.let { issuedYear ->
                    if (issuedYear in 1900..2100) year = issuedYear
                }
            }
        }

        // Ensure published is only "where published", not full citation with title.
        published = normalizeWherePublished(published)
        // If venue line has no year but we know the publication year, add it (requirement: show where published clearly).
        if (!Regex("\\b(19|20)\\d{2}\\b").containsMatchIn(published) && year in 1900..2100) {
            published = "$published ($year)".trim()
        }

        return if (authors != pub.authors || published != pub.published || year != pub.year) {
            pub.copy(authors = authors, published = published, year = year)
        } else {
            pub
        }
    }

    private fun looksLikeOrgAuthors(value: String): Boolean =
        value.contains("ИАиЭ", ignoreCase = true) ||
            value.contains("институт", ignoreCase = true) ||
            value.contains("лаборатор", ignoreCase = true) ||
            value.count { it == ',' } == 0 && value.length < 10

    private fun looksLikePlaceholderPublished(value: String): Boolean =
        value.contains("лаборатория", ignoreCase = true) ||
            value.contains("источник:", ignoreCase = true) ||
            value.contains("iae.nsk.su", ignoreCase = true)

    private fun extractAuthorsFromPdfText(text: String): String? {
        if (text.isBlank()) return null

        // Look for a run of name-like tokens near the end of the first paragraph or near the title block.
        val head = text.take(2000)

        // RU: "Фамилия И.О." or "И.О. Фамилия"
        val ru1 = Regex("\\b[А-ЯЁ][а-яё\\-]+\\s+[А-ЯЁ]\\.?\\s*[А-ЯЁ]\\.?", RegexOption.IGNORE_CASE)
        val ru2 = Regex("\\b[А-ЯЁ]\\.?\\s*[А-ЯЁ]\\.?\\s+[А-ЯЁ][а-яё\\-]+\\b", RegexOption.IGNORE_CASE)
        val en = Regex("\\b[A-Z][a-z\\-]+\\s+[A-Z]\\.?\\s*[A-Z]\\.?", RegexOption.IGNORE_CASE)

        fun bestLine(regexes: List<Regex>): String? {
            val candidates = head.split(Regex("[\\r\\n]+|\\s{2,}"))
                .map { it.trim() }
                .filter { it.length in 10..220 }

            val scored = candidates.map { line ->
                val matches = regexes.sumOf { re -> re.findAll(line).count() }
                line to matches
            }.filter { it.second >= 1 }

            val best = scored.maxByOrNull { it.second }?.first ?: return null
            val names = (regexes.flatMap { it.findAll(best).map { m -> m.value } }.toList())
                .distinct()
            return names.joinToString(", ").takeIf { it.isNotBlank() }
        }

        return bestLine(listOf(ru1, ru2, en))
    }

    private fun extractPublishedFromPdfText(text: String, theme: String): String? {
        if (text.isBlank()) return null
        val normalizedThemeTokens = theme.lowercase()
            .split(Regex("[^a-zа-яё0-9]+"))
            .filter { it.length >= 4 }
            .take(8)

        // Find bibliography-like entries. Prefer numbered list items.
        val candidates = mutableListOf<String>()

        Regex("(?is)\\bПубликации\\b\\s*:?(.{0,4000})").find(text)?.groupValues?.getOrNull(1)?.let { tail ->
            candidates += splitBibliographyChunks(tail)
        }
        // Also scan overall text for numbered chunks like "1. ...".
        candidates += splitBibliographyChunks(text)

        if (candidates.isEmpty()) return null

        val best = candidates
            .distinct()
            .map { c -> c to normalizedThemeTokens.count { t -> c.lowercase().contains(t) } }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenByDescending { it.first.length })
            .firstOrNull { it.second > 0 }?.first
            ?: candidates.first()

        return normalizeWherePublished(best).takeIf { looksLikeVenue(it) }
    }

    private fun splitBibliographyChunks(text: String): List<String> {
        val compact = text.replace(Regex("\\s+"), " ").trim()
        if (compact.isBlank()) return emptyList()
        val parts = compact.split(Regex("(?=\\b\\d+\\.)"))
            .map { it.trim() }
            .filter { it.matches(Regex("^\\d+\\..*")) }
            .map { it.replace(Regex("^\\d+\\s*\\."), "").trim() }
            .map { it.replace(Regex("(?is)\\s+(Разработан|Разработана|Разработано|Разработаны|Впервые|Продемонстр\\w*|В работе|В статье|Показано|Рассмотрено|Описание|Для решения|Получен\\w*|Достигнут\\w*).*$"), "").trim() }
        return parts
    }

    private fun extractDoi(value: String): String? {
        val trimmed = value.trim()
        // explicit doi.org form
        Regex("(?i)doi\\.org/\\s*(10\\.\\d{4,9}/[^\\s\\)\\]\\}\"'>]+)").find(trimmed)?.let { m ->
            return m.groupValues[1].trimEnd('.', ',', ';', ':', ')', ']', '}', '"', '\'')
        }
        // DOI: 10.x/...
        Regex("(?i)\\bdoi\\s*:?\\s*(10\\.\\d{4,9}/[^\\s\\)\\]\\}\"'>]+)").find(trimmed)?.let { m ->
            return m.groupValues[1].trimEnd('.', ',', ';', ':', ')', ']', '}', '"', '\'')
        }
        // plain DOI
        Regex("\\b10\\.\\d{4,9}/[^\\s\\)\\]\\}\"'>]+").find(trimmed)?.let { m ->
            return m.value.trimEnd('.', ',', ';', ':', ')', ']', '}', '"', '\'')
        }
        return null
    }

    private fun formatVenue(meta: CrossrefWorkMeta): String {
        val parts = mutableListOf<String>()
        meta.containerTitle?.let { parts += it }
        meta.year?.let { parts += it.toString() }

        val volIssue = buildString {
            meta.volume?.let { append("Vol. ").append(it) }
            meta.issue?.let {
                if (isNotBlank()) append("(")
                append(it)
                if (meta.volume != null) append(")")
            }
        }.trim().takeIf { it.isNotBlank() }
        if (volIssue != null) parts += volIssue

        meta.page?.let { parts += "pp. $it" }
        meta.articleNumber?.let { parts += "Article $it" }
        meta.doi?.let { parts += "DOI: $it" }

        return parts.joinToString(". ").replace(Regex("\\.\\s*\\."), ".").trim().trimEnd('.')
    }

    private fun normalizeWherePublished(value: String): String {
        var s = value.replace(Regex("\\s+"), " ").trim()
        // Prefer part after '//' when present.
        if (s.contains("//")) {
            s = s.substringAfter("//").trim()
        } else {
            // Prefer part after quotes (title removal).
            val afterQuote = s.substringAfterLast("\"", missingDelimiterValue = s)
            if (afterQuote != s) s = afterQuote.trim().trimStart(',', '–', '-', '.', ':').trim()
            val afterRuQuote = s.substringAfterLast("»", missingDelimiterValue = s)
            if (afterRuQuote != s) s = afterRuQuote.trim().trimStart(',', '–', '-', '.', ':').trim()

            // If still starts with authors/title, try to cut from a known venue marker.
            val venueMarkers = listOf(
                "Journal", "J.", "Proc.", "Proceedings", "Lecture Notes", "LNCS", "IEEE", "ACM", "Springer", "Elsevier",
                "Opt.", "Optics", "Applied", "Physical", "Photonics", "SPIE", "Письма", "ЖЭТФ", "Автометрия"
            )
            val idx = venueMarkers.map { marker ->
                val i = s.indexOf(marker, ignoreCase = true)
                if (i >= 0) i else Int.MAX_VALUE
            }.minOrNull() ?: Int.MAX_VALUE
            if (idx in 5..(s.length - 5)) {
                s = s.substring(idx).trim().trimStart(',', '–', '-', '.', ':').trim()
            }
        }
        // Drop dangling DOI label with no value.
        s = s.replace(Regex("(?i)\\bdoi\\s*:\\s*$"), "").trim()
        return s
    }

    private fun looksLikeVenue(value: String): Boolean {
        if (value.length < 8) return false
        return Regex("(doi|10\\.|журнал|вестник|известия|труды|материалы|сборник|том|№|pp\\.|\\bс\\.|conference|proceedings|symposium|пат\\.|pat\\.|ieee|acm|springer|elsevier|lncs|spie|opt\\.|optics)", RegexOption.IGNORE_CASE)
            .containsMatchIn(value)
    }

    private fun isValid(pub: com.example.poprogknowledgebaseback.domain.publication.Publication): Boolean {
        val personPattern = Regex(
            "([А-ЯЁ][а-яё\\-]+\\d*\\s*[А-ЯЁ]\\.\\s*[А-ЯЁ]\\.?)" + // Surname1 I.O.
                "|([А-ЯЁ][а-яё\\-]+\\d*\\s*[А-ЯЁ]\\.)" + // Surname1 I.
                "|([А-ЯЁ]\\.\\s*[А-ЯЁ]\\.\\s*[А-ЯЁ][а-яё\\-]+\\d*)" + // I.O. Surname1
                "|([А-ЯЁ]\\.\\s*[А-ЯЁ][а-яё\\-]+\\d*)" + // I. Surname1
                "|([A-Z][a-z\\-]+\\d*\\s*[A-Z]\\.\\s*[A-Z]\\.?)" + // Surname1 I.O.
                "|([A-Z][a-z\\-]+\\d*\\s*[A-Z]\\.)" + // Surname1 I.
                "|([A-Z]\\.\\s*[A-Z]\\.\\s*[A-Z][a-z\\-]+\\d*)" + // I.O. Surname1
                "|([A-Z]\\.\\s*[A-Z][a-z\\-]+\\d*)" // I. Surname1
        )
        val authorsOk = pub.authors.isNotBlank() &&
            !looksLikeOrgAuthors(pub.authors) &&
            personPattern.containsMatchIn(pub.authors)
        val themeOk = pub.theme.isNotBlank() && pub.theme.length >= 8
        val publishedOk = pub.published.isNotBlank() &&
            looksLikeVenue(pub.published) &&
            (Regex("\\b(19|20)\\d{2}\\b").containsMatchIn(pub.published) || pub.year in 1900..2100)
        return authorsOk && themeOk && publishedOk
    }
}

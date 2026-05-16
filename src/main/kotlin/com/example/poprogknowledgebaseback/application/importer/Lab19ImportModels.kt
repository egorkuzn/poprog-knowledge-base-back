package com.example.poprogknowledgebaseback.application.importer

data class Lab19ImportCommand(
    val dryRun: Boolean = true,
    val maxPages: Int = 30,
    val maxDepth: Int = 2,
    val importBaseUrl: String = "http://localhost:8080/api"
)

data class Lab19ImportReport(
    val startUrl: String,
    val dryRun: Boolean,
    val visitedPages: Int,
    val discoveredLinks: Int,
    val workingLinks: Int,
    val brokenLinks: Int,
    val ambiguousLinks: Int,
    val pdfCandidates: List<Lab19PdfCandidate>,
    val importedNewsItems: List<Lab19ImportedNewsItem>,
    val importedPublications: List<Lab19ImportedPublication>,
    val importedStudentWorks: List<Lab19ImportedStudentWork>,
    val linkAudit: List<Lab19LinkAuditItem>
)

data class Lab19PdfCandidate(
    val url: String,
    val sourcePage: String,
    val title: String,
    val year: Int?,
    val status: String,
    val contentType: String?,
    val materialKind: String
)

data class Lab19ImportedPublication(
    val sourceUrl: String,
    val publicationId: Long?,
    val title: String,
    val status: String,
    val error: String? = null
)

data class Lab19ImportedStudentWork(
    val sourceUrl: String,
    val workId: Long?,
    val title: String,
    val status: String,
    val error: String? = null
)

data class Lab19ImportedNewsItem(
    val sourceUrl: String,
    val newsItemId: Long?,
    val title: String,
    val status: String,
    val error: String? = null
)

data class Lab19LinkAuditItem(
    val url: String,
    val sourcePage: String,
    val label: String,
    val status: String,
    val httpStatus: Int? = null,
    val contentType: String? = null,
    val error: String? = null
)

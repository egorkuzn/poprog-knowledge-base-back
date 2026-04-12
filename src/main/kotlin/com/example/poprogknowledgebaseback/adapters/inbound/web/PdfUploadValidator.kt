package com.example.poprogknowledgebaseback.adapters.inbound.web

import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

private val pdfContentTypes = setOf(
    "application/pdf",
    "application/x-pdf"
)

fun requirePdfUpload(file: MultipartFile) {
    val originalName = file.originalFilename?.trim().orEmpty().lowercase()
    val contentType = file.contentType?.trim()?.lowercase().orEmpty()

    val fileNameLooksPdf = originalName.endsWith(".pdf")
    val contentTypeLooksPdf = contentType in pdfContentTypes

    if (!fileNameLooksPdf && !contentTypeLooksPdf) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are supported for upload")
    }
}

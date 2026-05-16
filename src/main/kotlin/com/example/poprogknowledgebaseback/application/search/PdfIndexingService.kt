package com.example.poprogknowledgebaseback.application.search

import com.example.poprogknowledgebaseback.application.files.FileStoragePathResolver
import com.example.poprogknowledgebaseback.application.files.FileStorageUseCase
import com.example.poprogknowledgebaseback.application.files.PdfTextExtractor
import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class PdfIndexingService(
    private val fileStoragePathResolver: FileStoragePathResolver,
    private val fileStorageUseCase: FileStorageUseCase,
    private val pdfTextExtractor: PdfTextExtractor,
    private val publicationPersistencePort: PublicationPersistencePort,
    private val studentWorkPersistencePort: StudentWorkPersistencePort,
    private val searchUseCase: SearchUseCase
) {

    @Async
    fun indexPublicationPdf(publication: Publication) {
        val publicationId = publication.id ?: return
        val current = publicationPersistencePort.findById(publicationId) ?: return
        if (!current.pdfText.isNullOrBlank()) {
            return
        }

        val extracted = extractPdfText(current.link) ?: return
        val updated = publicationPersistencePort.save(current.copy(pdfText = extracted))
        searchUseCase.indexPublication(updated)
    }

    @Async
    fun indexStudentWorkPdf(studentWork: StudentWork) {
        val workId = studentWork.id ?: return
        val current = studentWorkPersistencePort.findById(workId) ?: return
        if (!current.pdfText.isNullOrBlank()) {
            return
        }

        val extracted = extractPdfText(current.documentLink) ?: return
        val updated = studentWorkPersistencePort.save(current.copy(pdfText = extracted))
        searchUseCase.indexStudentWork(updated)
    }

    private fun extractPdfText(link: String?): String? {
        val normalizedLink = link?.trim().orEmpty()
        if (normalizedLink.isBlank() || !normalizedLink.lowercase().endsWith(".pdf")) {
            return null
        }

        val storedContent = fileStorageUseCase.loadFromUrl(normalizedLink)
        if (storedContent != null) {
            return pdfTextExtractor.extractText(storedContent.content)
        }

        val localPath = fileStoragePathResolver.resolveLocalPath(normalizedLink) ?: return null
        return pdfTextExtractor.extractText(localPath)
    }
}

package com.example.poprogknowledgebaseback.application.importer

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.lab19.SpringDataLab19NewsItemRepository
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.publication.SpringDataPublicationRepository
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.studentwork.SpringDataStudentWorkRepository
import com.example.poprogknowledgebaseback.application.lab19.Lab19NewsService
import com.example.poprogknowledgebaseback.domain.publication.port.PublicationPersistencePort
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Lab19MetadataSyncService(
    private val lab19Repository: SpringDataLab19NewsItemRepository,
    private val lab19NewsService: Lab19NewsService,
    private val publicationRepository: SpringDataPublicationRepository,
    private val studentWorkRepository: SpringDataStudentWorkRepository,
    private val publicationPersistencePort: PublicationPersistencePort,
    private val studentWorkPersistencePort: StudentWorkPersistencePort,
    private val importService: Lab19ImportService
) {

    @Transactional
    fun syncMetadata(limit: Int = 200): Lab19MetadataSyncReport {
        // Ensure staging classification is up-to-date before syncing.
        lab19NewsService.reclassifyAll()

        val items = lab19Repository.findAll()
            .filter { it.materialKind == Lab19MaterialClassifier.SCIENTIFIC_PUBLICATION || it.materialKind == Lab19MaterialClassifier.STUDENT_WORK }
            .sortedByDescending { it.id ?: 0L }
            .take(limit.coerceIn(1, 2000))

        var publicationsUpdated = 0
        var worksUpdated = 0
        val errors = mutableListOf<String>()

        items.forEach { item ->
            // Backfill KB entity refs for rows imported before kb_* columns existed.
            if (item.materialKind == Lab19MaterialClassifier.SCIENTIFIC_PUBLICATION && item.kbPublicationId == null) {
                val resolved = publicationRepository
                    .findByThemeAndPublishedContains(item.title, item.sourcePage, PageRequest.of(0, 1))
                    .firstOrNull()
                    ?.id
                if (resolved != null) {
                    item.kbPublicationId = resolved
                }
            }
            if (item.materialKind == Lab19MaterialClassifier.STUDENT_WORK && item.kbStudentWorkId == null) {
                val resolved = studentWorkRepository
                    .findByThemeAndPublishedContains(item.title, item.sourcePage, PageRequest.of(0, 1))
                    .firstOrNull()
                    ?.id
                if (resolved != null) {
                    item.kbStudentWorkId = resolved
                }
            }

            val sourceUrl = item.sourceUrl
            val pdfBytes = runCatching { importService.downloadPdfForSync(sourceUrl) }.getOrElse { ex ->
                errors += "download failed: $sourceUrl (${ex.message})"
                return@forEach
            }
            val pdfMeta = importService.extractPdfMetaForSync(pdfBytes, item.title)

            if (item.materialKind == Lab19MaterialClassifier.SCIENTIFIC_PUBLICATION && item.kbPublicationId != null) {
                val current = publicationPersistencePort.findById(item.kbPublicationId!!)
                if (current != null) {
                    val newAuthors = pdfMeta.authors ?: current.authors
                    val newPublished = pdfMeta.published ?: current.published
                    if (newAuthors != current.authors || newPublished != current.published) {
                        publicationPersistencePort.save(current.copy(authors = newAuthors, published = newPublished))
                        publicationsUpdated++
                    }
                }
            }

            if (item.materialKind == Lab19MaterialClassifier.STUDENT_WORK && item.kbStudentWorkId != null) {
                val current = studentWorkPersistencePort.findById(item.kbStudentWorkId!!)
                if (current != null) {
                    val newAuthors = pdfMeta.authors ?: current.authors
                    val newPublished = pdfMeta.published ?: current.published
                    if (newAuthors != current.authors || newPublished != current.published) {
                        studentWorkPersistencePort.save(current.copy(authors = newAuthors, published = newPublished))
                        worksUpdated++
                    }
                }
            }
        }

        lab19Repository.saveAll(items)

        return Lab19MetadataSyncReport(
            scanned = items.size,
            publicationsUpdated = publicationsUpdated,
            studentWorksUpdated = worksUpdated,
            errors = errors
        )
    }
}

data class Lab19MetadataSyncReport(
    val scanned: Int,
    val publicationsUpdated: Int,
    val studentWorksUpdated: Int,
    val errors: List<String>
)

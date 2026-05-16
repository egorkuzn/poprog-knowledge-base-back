package com.example.poprogknowledgebaseback.application.studentwork

import com.example.poprogknowledgebaseback.application.search.PdfIndexingService
import com.example.poprogknowledgebaseback.application.search.SearchUseCase
import com.example.poprogknowledgebaseback.domain.studentwork.ProjectTypeNotFoundException
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWorkNotFoundException
import com.example.poprogknowledgebaseback.domain.studentwork.WorkModel
import com.example.poprogknowledgebaseback.domain.studentwork.WorksByProjectType
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StudentWorkService(
    private val studentWorkPersistencePort: StudentWorkPersistencePort,
    private val searchUseCase: SearchUseCase,
    private val pdfIndexingService: PdfIndexingService
) : StudentWorkUseCase {

    @Transactional(readOnly = true)
    override fun getGroupedWorks(): List<WorksByProjectType> {
        val works = studentWorkPersistencePort.findAllOrdered()

        return works
            .groupBy { it.projectTypeHash }
            .map { (_, groupedWorks) ->
                val first = groupedWorks.first()
                WorksByProjectType(
                    title = first.projectTypeTitle,
                    hash = first.projectTypeHash,
                    works = groupedWorks.map {
                        WorkModel(
                            id = it.id ?: error("Student work id is missing"),
                            authors = it.authors,
                            theme = normalizeWorkTheme(it.theme),
                            published = it.published
                        )
                    }
                )
            }
    }

    @Transactional
    override fun create(command: UpsertStudentWorkCommand): StudentWorkResult {
        val projectType = studentWorkPersistencePort.findProjectTypeByHash(command.projectTypeHash)
            ?: throw ProjectTypeNotFoundException(command.projectTypeHash)

        val saved = studentWorkPersistencePort.save(
            StudentWork(
                id = null,
                projectTypeTitle = projectType.title,
                projectTypeHash = projectType.hash,
                authors = command.authors,
                theme = command.theme,
                published = command.published,
                documentLink = command.documentLink,
                pdfText = null
            )
        )

        searchUseCase.indexStudentWork(saved)
        pdfIndexingService.indexStudentWorkPdf(saved)
        return saved.toResult()
    }

    @Transactional
    override fun update(id: Long, command: UpsertStudentWorkCommand): StudentWorkResult {
        val current = studentWorkPersistencePort.findById(id)
            ?: throw StudentWorkNotFoundException(id)

        val projectType = studentWorkPersistencePort.findProjectTypeByHash(command.projectTypeHash)
            ?: throw ProjectTypeNotFoundException(command.projectTypeHash)

        val updated = studentWorkPersistencePort.save(
            current.copy(
                projectTypeTitle = projectType.title,
                projectTypeHash = projectType.hash,
                authors = command.authors,
                theme = command.theme,
                published = command.published,
                documentLink = command.documentLink ?: current.documentLink,
                pdfText = if (command.documentLink != null && command.documentLink != current.documentLink) null else current.pdfText
            )
        )

        searchUseCase.indexStudentWork(updated)
        pdfIndexingService.indexStudentWorkPdf(updated)
        return updated.toResult()
    }

    @Transactional
    override fun delete(id: Long) {
        val current = studentWorkPersistencePort.findById(id)
            ?: throw StudentWorkNotFoundException(id)
        studentWorkPersistencePort.deleteById(current.id ?: id)
        searchUseCase.removeStudentWork(current.id ?: id)
    }

    private fun StudentWork.toResult() = StudentWorkResult(
        id = id ?: error("Entity id was not generated"),
        projectTypeTitle = projectTypeTitle,
        projectTypeHash = projectTypeHash,
        authors = authors,
        theme = normalizeWorkTheme(theme),
        published = published,
        documentLink = documentLink
    )

    private fun normalizeWorkTheme(value: String): String {
        var s = value.replace(Regex("\\s+"), " ").trim()
        // Remove obvious artifacts from file naming / scraping.
        s = s.replace(Regex("\\s*\\((pdf|PDF)\\)\\s*$"), "")
            .replace(Regex("\\s*\\[\\s*(pdf|PDF)\\s*\\]\\s*$"), "")
            .replace(Regex("\\s*\\.pdf\\s*$", RegexOption.IGNORE_CASE), "")
            .trim()

        // If we have a Russian title followed by an English translation, keep only the Russian part.
        // Heuristic: first Latin letter that appears after any Cyrillic letters.
        val hasCyr = s.any { it in 'А'..'я' || it == 'ё' || it == 'Ё' }
        if (hasCyr) {
            val firstLatin = s.indexOfFirst { it in 'A'..'Z' || it in 'a'..'z' }
            if (firstLatin in 10..(s.length - 5)) {
                s = s.substring(0, firstLatin).trim().trimEnd('-', '–', ':', ';', ',', '.').trim()
            }
        }

        return s.ifBlank { value.trim() }
    }
}

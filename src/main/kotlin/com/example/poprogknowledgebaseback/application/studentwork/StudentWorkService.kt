package com.example.poprogknowledgebaseback.application.studentwork

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
    private val studentWorkPersistencePort: StudentWorkPersistencePort
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
                            authors = it.authors,
                            theme = it.theme,
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
                published = command.published
            )
        )

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
                published = command.published
            )
        )

        return updated.toResult()
    }

    @Transactional
    override fun delete(id: Long) {
        val current = studentWorkPersistencePort.findById(id)
            ?: throw StudentWorkNotFoundException(id)
        studentWorkPersistencePort.deleteById(current.id ?: id)
    }

    private fun StudentWork.toResult() = StudentWorkResult(
        id = id ?: error("Entity id was not generated"),
        projectTypeTitle = projectTypeTitle,
        projectTypeHash = projectTypeHash,
        authors = authors,
        theme = theme,
        published = published
    )
}

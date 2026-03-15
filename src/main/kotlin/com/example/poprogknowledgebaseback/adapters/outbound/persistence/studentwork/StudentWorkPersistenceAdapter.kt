package com.example.poprogknowledgebaseback.adapters.outbound.persistence.studentwork

import com.example.poprogknowledgebaseback.domain.studentwork.ProjectType
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork
import com.example.poprogknowledgebaseback.domain.studentwork.port.StudentWorkPersistencePort
import org.springframework.stereotype.Component

@Component
class StudentWorkPersistenceAdapter(
    private val studentWorkRepository: SpringDataStudentWorkRepository,
    private val projectTypeRepository: SpringDataProjectTypeRepository
) : StudentWorkPersistencePort {

    override fun findAllOrdered(): List<StudentWork> =
        studentWorkRepository.findAllOrdered().map { it.toDomain() }

    override fun findById(id: Long): StudentWork? =
        studentWorkRepository.findByIdWithProjectType(id)?.toDomain()

    override fun save(studentWork: StudentWork): StudentWork {
        val projectType = projectTypeRepository.findByHash(studentWork.projectTypeHash)
            ?: error("Project type hash does not exist: ${studentWork.projectTypeHash}")

        val entity = StudentWorkJpaEntity(
            id = studentWork.id,
            projectType = projectType,
            authors = studentWork.authors,
            theme = studentWork.theme,
            published = studentWork.published
        )

        return studentWorkRepository.save(entity).let {
            StudentWork(
                id = it.id,
                projectTypeTitle = projectType.title,
                projectTypeHash = projectType.hash,
                authors = it.authors,
                theme = it.theme,
                published = it.published
            )
        }
    }

    override fun deleteById(id: Long) {
        studentWorkRepository.deleteById(id)
    }

    override fun findProjectTypeByHash(hash: String): ProjectType? =
        projectTypeRepository.findByHash(hash)?.let { ProjectType(title = it.title, hash = it.hash) }

    private fun StudentWorkJpaEntity.toDomain() = StudentWork(
        id = id,
        projectTypeTitle = projectType.title,
        projectTypeHash = projectType.hash,
        authors = authors,
        theme = theme,
        published = published
    )
}

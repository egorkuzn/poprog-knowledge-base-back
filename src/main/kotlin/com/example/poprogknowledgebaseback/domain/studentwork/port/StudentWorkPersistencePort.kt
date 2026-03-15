package com.example.poprogknowledgebaseback.domain.studentwork.port

import com.example.poprogknowledgebaseback.domain.studentwork.ProjectType
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork

interface StudentWorkPersistencePort {
    fun findAllOrdered(): List<StudentWork>
    fun findById(id: Long): StudentWork?
    fun save(studentWork: StudentWork): StudentWork
    fun deleteById(id: Long)
    fun findProjectTypeByHash(hash: String): ProjectType?
}

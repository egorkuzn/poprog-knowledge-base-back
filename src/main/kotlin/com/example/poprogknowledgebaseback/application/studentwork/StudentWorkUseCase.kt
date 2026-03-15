package com.example.poprogknowledgebaseback.application.studentwork

import com.example.poprogknowledgebaseback.domain.studentwork.WorksByProjectType

interface StudentWorkUseCase {
    fun getGroupedWorks(): List<WorksByProjectType>
    fun create(command: UpsertStudentWorkCommand): StudentWorkResult
    fun update(id: Long, command: UpsertStudentWorkCommand): StudentWorkResult
    fun delete(id: Long)
}

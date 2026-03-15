package com.example.poprogknowledgebaseback.domain.studentwork

class StudentWorkNotFoundException(id: Long) : RuntimeException("Student work with id=$id not found")

class ProjectTypeNotFoundException(hash: String) : RuntimeException("Project type with hash=$hash not found")

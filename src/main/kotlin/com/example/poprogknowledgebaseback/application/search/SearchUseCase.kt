package com.example.poprogknowledgebaseback.application.search

interface SearchUseCase {
    fun search(query: String, limit: Int = 20): List<SearchResult>
    fun reindex()
    fun indexPublication(publication: com.example.poprogknowledgebaseback.domain.publication.Publication)
    fun indexStudentWork(studentWork: com.example.poprogknowledgebaseback.domain.studentwork.StudentWork)
    fun removePublication(id: Long)
    fun removeStudentWork(id: Long)
}

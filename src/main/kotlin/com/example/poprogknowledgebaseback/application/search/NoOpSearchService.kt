package com.example.poprogknowledgebaseback.application.search

import com.example.poprogknowledgebaseback.domain.publication.Publication
import com.example.poprogknowledgebaseback.domain.search.SearchItem
import com.example.poprogknowledgebaseback.domain.studentwork.StudentWork
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["app.search.enabled"], havingValue = "false")
class NoOpSearchService : SearchUseCase {
    override fun reindex() = Unit

    override fun search(query: String, limit: Int): List<SearchResult> =
        emptyList()

    override fun indexPublication(publication: Publication) = Unit

    override fun indexStudentWork(studentWork: StudentWork) = Unit

    override fun removePublication(id: Long) = Unit

    override fun removeStudentWork(id: Long) = Unit
}

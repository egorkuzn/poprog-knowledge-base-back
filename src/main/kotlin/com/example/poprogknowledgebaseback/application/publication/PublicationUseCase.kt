package com.example.poprogknowledgebaseback.application.publication

import com.example.poprogknowledgebaseback.domain.publication.PublicationsByDate

interface PublicationUseCase {
    fun getGroupedPublications(): List<PublicationsByDate>
    fun create(command: UpsertPublicationCommand): PublicationResult
    fun update(id: Long, command: UpsertPublicationCommand): PublicationResult
    fun delete(id: Long)
}

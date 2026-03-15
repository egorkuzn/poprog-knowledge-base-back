package com.example.poprogknowledgebaseback.domain.publication.port

import com.example.poprogknowledgebaseback.domain.publication.Publication

interface PublicationPersistencePort {
    fun findAllOrderByYearDescIdAsc(): List<Publication>
    fun findById(id: Long): Publication?
    fun save(publication: Publication): Publication
    fun deleteById(id: Long)
}

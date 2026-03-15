package com.example.poprogknowledgebaseback.domain.publication

class PublicationNotFoundException(id: Long) : RuntimeException("Publication with id=$id not found")

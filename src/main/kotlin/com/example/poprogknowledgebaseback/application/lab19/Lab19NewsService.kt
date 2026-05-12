package com.example.poprogknowledgebaseback.application.lab19

import com.example.poprogknowledgebaseback.adapters.outbound.persistence.lab19.Lab19NewsItemJpaEntity
import com.example.poprogknowledgebaseback.adapters.outbound.persistence.lab19.SpringDataLab19NewsItemRepository
import com.example.poprogknowledgebaseback.application.importer.Lab19MaterialClassifier
import java.time.OffsetDateTime
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Lab19NewsService(
    private val repository: SpringDataLab19NewsItemRepository
) {

    @Transactional(readOnly = true)
    fun findAll(): List<Lab19NewsItem> =
        repository.findAllByOrderByYearDescIdDesc().map { it.toModel() }

    @Transactional(readOnly = true)
    fun search(
        query: String?,
        year: Int?,
        materialKind: String?,
        limit: Int,
        offset: Int
    ): List<Lab19NewsItem> {
        val boundedLimit = limit.coerceIn(1, 200)
        val boundedOffset = offset.coerceAtLeast(0)
        val pageable = OffsetBasedPageRequest(
            offset = boundedOffset.toLong(),
            limit = boundedLimit,
            sort = Sort.by(Sort.Order.desc("year"), Sort.Order.desc("id"))
        )

        return repository.search(
            query = query?.trim()?.takeIf { it.isNotBlank() },
            year = year,
            materialKind = materialKind?.trim()?.takeIf { it.isNotBlank() },
            pageable = pageable
        ).content.map { it.toModel() }
    }

    @Transactional
    fun upsert(command: UpsertLab19NewsItemCommand): Lab19NewsItem {
        val now = OffsetDateTime.now()
        val current = repository.findBySourceUrl(command.sourceUrl)
        val saved = repository.save(
            current?.apply {
                title = command.title
                sourcePage = command.sourcePage
                year = command.year
                contentType = command.contentType
                materialKind = command.materialKind
                status = command.status
                updatedAt = now
            } ?: Lab19NewsItemJpaEntity(
                title = command.title,
                sourceUrl = command.sourceUrl,
                sourcePage = command.sourcePage,
                year = command.year,
                contentType = command.contentType,
                materialKind = command.materialKind,
                status = command.status,
                createdAt = now,
                updatedAt = now
            )
        )

        return saved.toModel()
    }

    @Transactional
    fun reclassifyAll(): Int {
        val items = repository.findAll()
        var updated = 0
        items.forEach { entity ->
            val newKind = Lab19MaterialClassifier.classify(
                sourcePage = entity.sourcePage,
                title = entity.title,
                url = entity.sourceUrl
            )
            if (entity.materialKind != newKind) {
                entity.materialKind = newKind
                entity.updatedAt = OffsetDateTime.now()
                updated++
            }
        }
        if (updated > 0) {
            repository.saveAll(items)
        }
        return updated
    }

    private fun Lab19NewsItemJpaEntity.toModel() = Lab19NewsItem(
        id = id ?: error("Lab19 news item id is missing"),
        title = title,
        sourceUrl = sourceUrl,
        sourcePage = sourcePage,
        year = year,
        contentType = contentType,
        materialKind = materialKind,
        status = status
    )
}

data class UpsertLab19NewsItemCommand(
    val title: String,
    val sourceUrl: String,
    val sourcePage: String,
    val year: Int?,
    val contentType: String?,
    val materialKind: String,
    val status: String
)

private class OffsetBasedPageRequest(
    private val offset: Long,
    private val limit: Int,
    private val sort: Sort
) : org.springframework.data.domain.Pageable {
    init {
        require(offset >= 0) { "Offset must be non-negative" }
        require(limit > 0) { "Limit must be greater than zero" }
    }

    override fun getPageNumber(): Int =
        if (limit == 0) 0 else (offset / limit).toInt()

    override fun getPageSize(): Int = limit

    override fun getOffset(): Long = offset

    override fun getSort(): Sort = sort

    override fun next(): org.springframework.data.domain.Pageable =
        OffsetBasedPageRequest(offset + limit, limit, sort)

    override fun previousOrFirst(): org.springframework.data.domain.Pageable =
        if (hasPrevious()) OffsetBasedPageRequest(offset - limit, limit, sort) else first()

    override fun first(): org.springframework.data.domain.Pageable =
        OffsetBasedPageRequest(0, limit, sort)

    override fun withPage(pageNumber: Int): org.springframework.data.domain.Pageable =
        OffsetBasedPageRequest(pageNumber.toLong() * limit, limit, sort)

    override fun hasPrevious(): Boolean = offset > 0
}

package com.example.poprogknowledgebaseback.adapters.outbound.persistence.projectmenu

import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuItem
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuPromo
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuSection
import com.example.poprogknowledgebaseback.domain.projectmenu.port.ProjectMenuPersistencePort
import org.springframework.stereotype.Component

@Component
class ProjectMenuPersistenceAdapter(
    private val sectionRepository: SpringDataProjectMenuSectionRepository,
    private val itemRepository: SpringDataProjectMenuItemRepository,
    private val promoRepository: SpringDataProjectMenuPromoRepository
) : ProjectMenuPersistencePort {

    override fun findAllSectionsOrderBySortOrderAscIdAsc(): List<ProjectMenuSection> =
        sectionRepository.findAllByOrderBySortOrderAscIdAsc().map { it.toDomain() }

    override fun findItemsBySectionIds(sectionIds: List<Long>): List<ProjectMenuItem> =
        if (sectionIds.isEmpty()) emptyList()
        else itemRepository.findAllBySectionIdInOrderBySortOrderAscIdAsc(sectionIds).map { it.toDomain() }

    override fun findPromosBySectionIds(sectionIds: List<Long>): List<ProjectMenuPromo> =
        if (sectionIds.isEmpty()) emptyList()
        else promoRepository.findAllBySectionIdInOrderBySortOrderAscIdAsc(sectionIds).map { it.toDomain() }

    override fun findSectionById(id: Long): ProjectMenuSection? =
        sectionRepository.findById(id).orElse(null)?.toDomain()

    override fun findSectionByHash(hash: String): ProjectMenuSection? =
        sectionRepository.findByHash(hash)?.toDomain()

    override fun saveSection(section: ProjectMenuSection): ProjectMenuSection =
        sectionRepository.save(section.toEntity()).toDomain()

    override fun deleteSectionById(id: Long) {
        sectionRepository.deleteById(id)
    }

    override fun findItemById(id: Long): ProjectMenuItem? =
        itemRepository.findById(id).orElse(null)?.toDomain()

    override fun saveItem(item: ProjectMenuItem): ProjectMenuItem =
        itemRepository.save(item.toEntity()).toDomain()

    override fun deleteItemById(id: Long) {
        itemRepository.deleteById(id)
    }

    override fun findPromoById(id: Long): ProjectMenuPromo? =
        promoRepository.findById(id).orElse(null)?.toDomain()

    override fun savePromo(promo: ProjectMenuPromo): ProjectMenuPromo =
        promoRepository.save(promo.toEntity()).toDomain()

    override fun deletePromoById(id: Long) {
        promoRepository.deleteById(id)
    }

    private fun ProjectMenuSectionJpaEntity.toDomain() = ProjectMenuSection(
        id = id,
        hash = hash,
        title = title,
        description = description,
        ctaTitle = ctaTitle,
        ctaUrl = ctaUrl,
        sortOrder = sortOrder
    )

    private fun ProjectMenuSection.toEntity() = ProjectMenuSectionJpaEntity(
        id = id,
        hash = hash,
        title = title,
        description = description,
        ctaTitle = ctaTitle,
        ctaUrl = ctaUrl,
        sortOrder = sortOrder
    )

    private fun ProjectMenuItemJpaEntity.toDomain() = ProjectMenuItem(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        highlighted = highlighted,
        sortOrder = sortOrder
    )

    private fun ProjectMenuItem.toEntity() = ProjectMenuItemJpaEntity(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        highlighted = highlighted,
        sortOrder = sortOrder
    )

    private fun ProjectMenuPromoJpaEntity.toDomain() = ProjectMenuPromo(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sortOrder = sortOrder
    )

    private fun ProjectMenuPromo.toEntity() = ProjectMenuPromoJpaEntity(
        id = id,
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sortOrder = sortOrder
    )
}

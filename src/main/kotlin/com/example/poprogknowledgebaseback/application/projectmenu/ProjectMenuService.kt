package com.example.poprogknowledgebaseback.application.projectmenu

import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenu
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuItem
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuItemNotFoundException
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuPromo
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuPromoNotFoundException
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuSection
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuSectionHashAlreadyExistsException
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuSectionNotFoundException
import com.example.poprogknowledgebaseback.domain.projectmenu.port.ProjectMenuPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectMenuService(
    private val projectMenuPersistencePort: ProjectMenuPersistencePort
) : ProjectMenuUseCase {

    @Transactional(readOnly = true)
    override fun getMenu(): ProjectMenuResponseModel {
        val sections = projectMenuPersistencePort.findAllSectionsOrderBySortOrderAscIdAsc()
        val sectionIds = sections.mapNotNull { it.id }
        val itemsBySectionId = projectMenuPersistencePort.findItemsBySectionIds(sectionIds).groupBy { it.sectionId }
        val promosBySectionId = projectMenuPersistencePort.findPromosBySectionIds(sectionIds).groupBy { it.sectionId }

        val menu = ProjectMenu(
            title = "Проекты",
            sections = sections.map { section ->
                section.copy(
                    items = itemsBySectionId[section.id].orEmpty(),
                    promos = promosBySectionId[section.id].orEmpty()
                )
            }
        )

        return menu.toResult()
    }

    @Transactional
    override fun createSection(command: UpsertProjectMenuSectionCommand): ProjectMenuSectionResult {
        ensureSectionHashIsUnique(command.hash, null)
        return projectMenuPersistencePort.saveSection(command.toSection()).toResult()
    }

    @Transactional
    override fun updateSection(id: Long, command: UpsertProjectMenuSectionCommand): ProjectMenuSectionResult {
        val current = projectMenuPersistencePort.findSectionById(id)
            ?: throw ProjectMenuSectionNotFoundException(id)
        ensureSectionHashIsUnique(command.hash, id)

        return projectMenuPersistencePort.saveSection(
            current.copy(
                hash = command.hash,
                title = command.title,
                description = command.description,
                ctaTitle = command.ctaTitle,
                ctaUrl = command.ctaUrl,
                sortOrder = command.sortOrder
            )
        ).toResult()
    }

    @Transactional
    override fun deleteSection(id: Long) {
        projectMenuPersistencePort.findSectionById(id)
            ?: throw ProjectMenuSectionNotFoundException(id)
        projectMenuPersistencePort.deleteSectionById(id)
    }

    @Transactional
    override fun createItem(command: UpsertProjectMenuItemCommand): ProjectMenuItemResult {
        ensureSectionExists(command.sectionId)
        return projectMenuPersistencePort.saveItem(command.toItem()).toResult()
    }

    @Transactional
    override fun updateItem(id: Long, command: UpsertProjectMenuItemCommand): ProjectMenuItemResult {
        val current = projectMenuPersistencePort.findItemById(id)
            ?: throw ProjectMenuItemNotFoundException(id)
        ensureSectionExists(command.sectionId)

        return projectMenuPersistencePort.saveItem(
            current.copy(
                sectionId = command.sectionId,
                title = command.title,
                description = command.description,
                url = command.url,
                imageUrl = command.imageUrl,
                highlighted = command.highlighted,
                sortOrder = command.sortOrder
            )
        ).toResult()
    }

    @Transactional
    override fun deleteItem(id: Long) {
        projectMenuPersistencePort.findItemById(id)
            ?: throw ProjectMenuItemNotFoundException(id)
        projectMenuPersistencePort.deleteItemById(id)
    }

    @Transactional
    override fun createPromo(command: UpsertProjectMenuPromoCommand): ProjectMenuPromoResult {
        ensureSectionExists(command.sectionId)
        return projectMenuPersistencePort.savePromo(command.toPromo()).toResult()
    }

    @Transactional
    override fun updatePromo(id: Long, command: UpsertProjectMenuPromoCommand): ProjectMenuPromoResult {
        val current = projectMenuPersistencePort.findPromoById(id)
            ?: throw ProjectMenuPromoNotFoundException(id)
        ensureSectionExists(command.sectionId)

        return projectMenuPersistencePort.savePromo(
            current.copy(
                sectionId = command.sectionId,
                title = command.title,
                description = command.description,
                url = command.url,
                imageUrl = command.imageUrl,
                sortOrder = command.sortOrder
            )
        ).toResult()
    }

    @Transactional
    override fun deletePromo(id: Long) {
        projectMenuPersistencePort.findPromoById(id)
            ?: throw ProjectMenuPromoNotFoundException(id)
        projectMenuPersistencePort.deletePromoById(id)
    }

    private fun ensureSectionExists(sectionId: Long) {
        projectMenuPersistencePort.findSectionById(sectionId)
            ?: throw ProjectMenuSectionNotFoundException(sectionId)
    }

    private fun ensureSectionHashIsUnique(hash: String, currentId: Long?) {
        val existing = projectMenuPersistencePort.findSectionByHash(hash)
        if (existing != null && existing.id != currentId) {
            throw ProjectMenuSectionHashAlreadyExistsException(hash)
        }
    }

    private fun UpsertProjectMenuSectionCommand.toSection() = ProjectMenuSection(
        hash = hash,
        title = title,
        description = description,
        ctaTitle = ctaTitle,
        ctaUrl = ctaUrl,
        sortOrder = sortOrder
    )

    private fun UpsertProjectMenuItemCommand.toItem() = ProjectMenuItem(
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        highlighted = highlighted,
        sortOrder = sortOrder
    )

    private fun UpsertProjectMenuPromoCommand.toPromo() = ProjectMenuPromo(
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sortOrder = sortOrder
    )

    private fun ProjectMenu.toResult() = ProjectMenuResponseModel(
        title = title,
        sections = sections.map { it.toResult() }
    )

    private fun ProjectMenuSection.toResult() = ProjectMenuSectionResult(
        id = id ?: error("Project menu section id was not generated"),
        hash = hash,
        title = title,
        description = description,
        ctaTitle = ctaTitle,
        ctaUrl = ctaUrl,
        sortOrder = sortOrder,
        items = items.sortedWith(compareBy<ProjectMenuItem> { it.sortOrder }.thenBy { it.id }).map { it.toResult() },
        promos = promos.sortedWith(compareBy<ProjectMenuPromo> { it.sortOrder }.thenBy { it.id }).map { it.toResult() }
    )

    private fun ProjectMenuItem.toResult() = ProjectMenuItemResult(
        id = id ?: error("Project menu item id was not generated"),
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        highlighted = highlighted,
        sortOrder = sortOrder
    )

    private fun ProjectMenuPromo.toResult() = ProjectMenuPromoResult(
        id = id ?: error("Project menu promo id was not generated"),
        sectionId = sectionId,
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        sortOrder = sortOrder
    )
}

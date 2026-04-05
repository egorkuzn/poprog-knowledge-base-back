package com.example.poprogknowledgebaseback.domain.projectmenu.port

import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuItem
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuPromo
import com.example.poprogknowledgebaseback.domain.projectmenu.ProjectMenuSection

interface ProjectMenuPersistencePort {
    fun findAllSectionsOrderBySortOrderAscIdAsc(): List<ProjectMenuSection>
    fun findItemsBySectionIds(sectionIds: List<Long>): List<ProjectMenuItem>
    fun findPromosBySectionIds(sectionIds: List<Long>): List<ProjectMenuPromo>

    fun findSectionById(id: Long): ProjectMenuSection?
    fun findSectionByHash(hash: String): ProjectMenuSection?
    fun saveSection(section: ProjectMenuSection): ProjectMenuSection
    fun deleteSectionById(id: Long)

    fun findItemById(id: Long): ProjectMenuItem?
    fun saveItem(item: ProjectMenuItem): ProjectMenuItem
    fun deleteItemById(id: Long)

    fun findPromoById(id: Long): ProjectMenuPromo?
    fun savePromo(promo: ProjectMenuPromo): ProjectMenuPromo
    fun deletePromoById(id: Long)
}

package com.example.poprogknowledgebaseback.application.projectmenu

interface ProjectMenuUseCase {
    fun getMenu(): ProjectMenuResponseModel
    fun createSection(command: UpsertProjectMenuSectionCommand): ProjectMenuSectionResult
    fun updateSection(id: Long, command: UpsertProjectMenuSectionCommand): ProjectMenuSectionResult
    fun deleteSection(id: Long)
    fun createItem(command: UpsertProjectMenuItemCommand): ProjectMenuItemResult
    fun updateItem(id: Long, command: UpsertProjectMenuItemCommand): ProjectMenuItemResult
    fun deleteItem(id: Long)
    fun createPromo(command: UpsertProjectMenuPromoCommand): ProjectMenuPromoResult
    fun updatePromo(id: Long, command: UpsertProjectMenuPromoCommand): ProjectMenuPromoResult
    fun deletePromo(id: Long)
}

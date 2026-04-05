package com.example.poprogknowledgebaseback.domain.projectmenu

class ProjectMenuSectionNotFoundException(id: Long) :
    RuntimeException("Project menu section with id=$id was not found")

class ProjectMenuItemNotFoundException(id: Long) :
    RuntimeException("Project menu item with id=$id was not found")

class ProjectMenuPromoNotFoundException(id: Long) :
    RuntimeException("Project menu promo with id=$id was not found")

class ProjectMenuSectionHashAlreadyExistsException(hash: String) :
    RuntimeException("Project menu section with hash='$hash' already exists")

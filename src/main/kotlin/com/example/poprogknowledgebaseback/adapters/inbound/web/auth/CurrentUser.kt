package com.example.poprogknowledgebaseback.adapters.inbound.web.auth

data class CurrentUser(
    val subject: String,
    val email: String?,
    val name: String?,
    val roles: Set<String>
) {
    fun hasRole(role: String): Boolean = roles.contains(role.uppercase())
}

package com.example.poprogknowledgebaseback.adapters.inbound.web.auth

import jakarta.servlet.http.HttpServletRequest

interface CurrentUserProvider {
    fun resolveOrNull(request: HttpServletRequest): CurrentUser?
}

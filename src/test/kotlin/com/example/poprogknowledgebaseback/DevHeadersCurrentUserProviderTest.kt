package com.example.poprogknowledgebaseback

import com.example.poprogknowledgebaseback.adapters.inbound.web.auth.DevHeadersCurrentUserProvider
import com.example.poprogknowledgebaseback.config.AuthDevHeadersProperties
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.server.ResponseStatusException

class DevHeadersCurrentUserProviderTest {

    @Test
    fun `should resolve user from headers in local profile`() {
        val properties = AuthDevHeadersProperties().apply {
            enabled = true
        }
        val environment = MockEnvironment().apply {
            setActiveProfiles("local")
        }
        val provider = DevHeadersCurrentUserProvider(properties, environment)
        val request = MockHttpServletRequest().apply {
            addHeader("subject", "user-123")
            addHeader("email", "user@example.com")
            addHeader("name", "User Name")
            addHeader("roles", "USER, ADMIN")
        }

        val user = provider.resolveOrNull(request)

        checkNotNull(user)
        assertEquals("user-123", user.subject)
        assertEquals("user@example.com", user.email)
        assertEquals("User Name", user.name)
        assertEquals(setOf("USER", "ADMIN"), user.roles)
    }

    @Test
    fun `should return null when subject is missing`() {
        val properties = AuthDevHeadersProperties().apply {
            enabled = true
        }
        val environment = MockEnvironment().apply {
            setActiveProfiles("local")
        }
        val provider = DevHeadersCurrentUserProvider(properties, environment)
        val request = MockHttpServletRequest()

        val user = provider.resolveOrNull(request)

        assertNull(user)
    }

    @Test
    fun `should reject usage outside allowed profiles`() {
        val properties = AuthDevHeadersProperties().apply {
            enabled = true
        }
        val environment = MockEnvironment().apply {
            setActiveProfiles("prod")
        }
        val provider = DevHeadersCurrentUserProvider(properties, environment)
        val request = MockHttpServletRequest().apply {
            addHeader("subject", "user-123")
        }

        val ex = assertThrows<ResponseStatusException> { provider.resolveOrNull(request) }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }
}

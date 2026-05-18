package com.example.poprogknowledgebaseback

import com.example.poprogknowledgebaseback.adapters.outbound.keycloak.KeycloakAdminRestClient
import com.example.poprogknowledgebaseback.application.account.RegisterAccountCommand
import com.example.poprogknowledgebaseback.config.AuthKeycloakProperties
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException

class KeycloakAdminRestClientTest {

    @Test
    fun `should create keycloak user and return created id`() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"access_token":"admin-token"}""")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .setHeader(
                        "Location",
                        "${server.url("/")}/admin/realms/reflex-ide/users/keycloak-user-123"
                    )
            )

            val client = KeycloakAdminRestClient(testProperties(server))

	            val userId = client.createUser(
	                RegisterAccountCommand(
	                    name = "Portal User",
	                    email = "portal-user@example.com",
	                    password = "StrongPass-123!"
	                )
	            )

            assertEquals("keycloak-user-123", userId)

            val tokenRequest = server.takeRequest()
            assertEquals("/realms/master/protocol/openid-connect/token", tokenRequest.path)
            assertTrue(tokenRequest.body.readUtf8().contains("username=admin"))

            val createRequest = server.takeRequest()
            assertEquals("/admin/realms/reflex-ide/users", createRequest.path)
	            assertEquals("Bearer admin-token", createRequest.getHeader("Authorization"))
	            val createBody = createRequest.body.readUtf8()
	            assertTrue(createBody.contains("portal-user@example.com"))
	            assertTrue(createBody.contains("StrongPass-123!"))
            assertTrue(createBody.contains(""""emailVerified":true"""))
            assertTrue(createBody.contains(""""firstName":"Portal""""))
            assertTrue(createBody.contains(""""lastName":"User""""))
        }
    }

    @Test
    fun `should translate keycloak conflict to conflict response`() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"access_token":"admin-token"}""")
            )
            server.enqueue(MockResponse().setResponseCode(409))

            val client = KeycloakAdminRestClient(testProperties(server))

            val ex = assertThrows<ResponseStatusException> {
	                client.createUser(
	                    RegisterAccountCommand(
	                        name = "Portal User",
	                        email = "portal-user@example.com",
	                        password = "StrongPass-123!"
	                    )
	                )
	            }

	            assertEquals(409, ex.statusCode.value())
	        }
	    }

    @Test
    fun `should translate keycloak bad request to bad request response`() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"access_token":"admin-token"}""")
            )
            server.enqueue(MockResponse().setResponseCode(400))

            val client = KeycloakAdminRestClient(testProperties(server))

            val ex = assertThrows<ResponseStatusException> {
                client.createUser(
                    RegisterAccountCommand(
                        name = "Portal User",
                        email = "portal-user@example.com",
                        password = "weak"
                    )
                )
            }

            assertEquals(400, ex.statusCode.value())
            assertTrue(ex.reason.orEmpty().contains("password policy", ignoreCase = true))
        }
    }

	    private fun testProperties(server: MockWebServer) =
        AuthKeycloakProperties().apply {
            enabled = true
            baseUrl = server.url("/").toString().trimEnd('/')
            realm = "reflex-ide"
            adminRealm = "master"
            adminClientId = "admin-cli"
            adminUsername = "admin"
            adminPassword = "admin-password"
        }
}

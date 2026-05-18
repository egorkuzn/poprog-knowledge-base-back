package com.example.poprogknowledgebaseback.adapters.outbound.keycloak

import com.example.poprogknowledgebaseback.application.account.KeycloakUserAdminClient
import com.example.poprogknowledgebaseback.application.account.RegisterAccountCommand
import com.example.poprogknowledgebaseback.config.AuthKeycloakProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException

@Component
class KeycloakAdminRestClient(
    private val properties: AuthKeycloakProperties
) : KeycloakUserAdminClient {

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(resolveBaseUrl())
            .build()
    }

    override fun createUser(command: RegisterAccountCommand): String {
        val token = requestAdminToken()
        val location = try {
            restClient.post()
                .uri("/admin/realms/{realm}/users", properties.realm)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .body(command.toKeycloakUserPayload())
                .exchange { _, response ->
                    when (response.statusCode.value()) {
                        HttpStatus.CREATED.value() -> response.headers.location
                        HttpStatus.BAD_REQUEST.value() -> throw ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Keycloak rejected account data. Check password policy and email format."
                        )
                        HttpStatus.CONFLICT.value() -> throw ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "User with this email already exists in Keycloak"
                        )
                        else -> throw ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Keycloak user creation failed with status ${response.statusCode.value()}"
                        )
                    }
                }
        } catch (ex: RestClientResponseException) {
            throw ResponseStatusException(
                if (ex.statusCode.value() == HttpStatus.BAD_REQUEST.value()) HttpStatus.BAD_REQUEST else HttpStatus.BAD_GATEWAY,
                if (ex.statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
                    "Keycloak rejected account data. Check password policy and email format."
                } else {
                    "Keycloak user creation failed with status ${ex.statusCode.value()}"
                },
                ex
            )
        }

        return location?.path
            ?.substringAfterLast("/")
            ?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak did not return created user id")
    }

    private fun requestAdminToken(): String {
        val username = properties.adminUsername.trim()
        val password = properties.adminPassword.trim()
        if (username.isBlank() || password.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak admin credentials are not configured")
        }

        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "password")
            add("client_id", properties.adminClientId)
            add("username", username)
            add("password", password)
        }

        val response = try {
            restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.adminRealm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse::class.java)
        } catch (ex: RestClientResponseException) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Keycloak admin token request failed with status ${ex.statusCode.value()}",
                ex
            )
        }

        return response?.accessToken?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak token response does not contain access_token")
    }

    private fun resolveBaseUrl(): String {
        val explicitBaseUrl = properties.baseUrl.trim().trimEnd('/')
        if (explicitBaseUrl.isNotBlank()) {
            return explicitBaseUrl
        }

        val issuerUri = properties.issuerUri.trim().trimEnd('/')
        if (issuerUri.isNotBlank()) {
            return issuerUri.substringBefore("/realms/")
        }

        throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak base URL is not configured")
    }

    private fun RegisterAccountCommand.toKeycloakUserPayload() = KeycloakCreateUserRequest(
        username = email,
        email = email,
        firstName = name.firstName(),
        lastName = name.lastName(),
        enabled = true,
        emailVerified = true,
        credentials = listOf(
            KeycloakCredentialRequest(
                type = "password",
                value = password,
                temporary = false
            )
        )
    )

    private fun String.firstName(): String =
        trim().split(Regex("\\s+"), limit = 2).firstOrNull().orEmpty().ifBlank { trim() }

    private fun String.lastName(): String =
        trim().split(Regex("\\s+"), limit = 2).getOrNull(1).orEmpty()

    @Suppress("unused")
    private data class KeycloakTokenResponse(
        val access_token: String? = null
    ) {
        val accessToken: String?
            get() = access_token
    }

    private data class KeycloakCreateUserRequest(
        val username: String,
        val email: String,
        val firstName: String,
        val lastName: String,
        val enabled: Boolean,
        val emailVerified: Boolean,
        val credentials: List<KeycloakCredentialRequest>
    )

    private data class KeycloakCredentialRequest(
        val type: String,
        val value: String,
        val temporary: Boolean
    )
}

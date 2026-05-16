package com.example.poprogknowledgebaseback.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.auth.keycloak")
class AuthKeycloakProperties {
    var enabled: Boolean = false
    var baseUrl: String = ""
    var issuerUri: String = ""
    var jwkSetUri: String = ""
    var realm: String = "reflex-ide"
    var clientId: String = "reflex-web-client"
    var requiredAudience: String = ""
    var adminRealm: String = "master"
    var adminClientId: String = "admin-cli"
    var adminUsername: String = ""
    var adminPassword: String = ""
}

package com.example.poprogknowledgebaseback.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.auth.keycloak")
class AuthKeycloakProperties {
    var enabled: Boolean = false
    var issuerUri: String = ""
    var jwkSetUri: String = ""
    var clientId: String = "reflex-web-client"
    var requiredAudience: String = ""
}

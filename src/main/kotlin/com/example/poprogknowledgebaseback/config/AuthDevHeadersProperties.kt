package com.example.poprogknowledgebaseback.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.auth.dev-headers")
class AuthDevHeadersProperties {
    var enabled: Boolean = false
    var subjectHeader: String = "subject"
    var emailHeader: String = "email"
    var nameHeader: String = "name"
    var rolesHeader: String = "roles"
    var allowedProfiles: List<String> = listOf("local", "dev")
}

package com.example.poprogknowledgebaseback.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.gigachat")
data class GigaChatProperties(
    var enabled: Boolean = false,
    var authUrl: String = "https://ngw.devices.sberbank.ru:9443",
    var apiUrl: String = "https://gigachat.devices.sberbank.ru",
    var authorizationKey: String = "",
    var scope: String = "GIGACHAT_API_PERS",
    var model: String = "GigaChat",
    var trustStorePath: String = "",
    var trustStorePassword: String = "",
    var trustStoreType: String = "PKCS12"
)

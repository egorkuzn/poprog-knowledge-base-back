package com.example.poprogknowledgebaseback.config

import java.io.FileInputStream
import java.security.KeyStore
import java.time.Clock
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(GigaChatProperties::class)
class GigaChatConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun restClientBuilder(properties: GigaChatProperties): RestClient.Builder {
        val requestFactory = HttpComponentsClientHttpRequestFactory(gigaChatHttpClient(properties))
        return RestClient.builder().requestFactory(requestFactory)
    }

    @Bean
    @ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "true")
    fun gigaChatAuthRestClient(
        builder: RestClient.Builder,
        properties: GigaChatProperties
    ): RestClient = builder
        .baseUrl(properties.authUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .build()

    @Bean
    @ConditionalOnProperty(name = ["app.gigachat.enabled"], havingValue = "true")
    fun gigaChatApiRestClient(
        builder: RestClient.Builder,
        properties: GigaChatProperties
    ): RestClient = builder
        .baseUrl(properties.apiUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    private fun gigaChatHttpClient(properties: GigaChatProperties): HttpClient {
        val trustStorePath = properties.trustStorePath.trim()
        if (trustStorePath.isBlank()) {
            return HttpClients.createDefault()
        }

        require(properties.trustStorePassword.isNotBlank()) {
            "GigaChat trust store password is required when trust store path is configured"
        }

        val trustStore = KeyStore.getInstance(properties.trustStoreType).apply {
            FileInputStream(trustStorePath).use { input ->
                load(input, properties.trustStorePassword.toCharArray())
            }
        }

        val sslContext = SSLContextBuilder()
            .loadTrustMaterial(trustStore, null)
            .build()

        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setTlsSocketStrategy(DefaultClientTlsStrategy(sslContext))
            .build()

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build()
    }
}

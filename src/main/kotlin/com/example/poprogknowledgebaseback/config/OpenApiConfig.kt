package com.example.poprogknowledgebaseback.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("POPROG Knowledge Base API")
                .version("v1")
                .description(
                    "API для работы с публикациями, студенческими работами, " +
                        "загрузкой документов и полнотекстовым поиском."
                )
        )
}

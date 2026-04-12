package com.example.poprogknowledgebaseback.adapters.inbound.web.auth

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUserParam(
    val required: Boolean = true
)

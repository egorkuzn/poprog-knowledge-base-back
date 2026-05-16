package com.example.poprogknowledgebaseback.adapters.inbound.web.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException

@Component
class CurrentUserArgumentResolver(
    private val currentUserProviders: List<CurrentUserProvider>
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == CurrentUser::class.java &&
            parameter.hasParameterAnnotation(CurrentUserParam::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val annotation = parameter.getParameterAnnotation(CurrentUserParam::class.java)
            ?: error("CurrentUserParam annotation must be present")
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: error("HttpServletRequest is required")

        val user = currentUserProviders.firstNotNullOfOrNull { it.resolveOrNull(request) }
        if (user == null && annotation.required) {
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required"
            )
        }
        return user
    }
}

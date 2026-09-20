package com.github.mrbean355.roons.security

import com.github.mrbean355.roons.service.MetadataService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthInterceptor(
    private val metadataService: MetadataService
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (handler !is HandlerMethod) {
            return true
        }

        val isAdminOnly = handler.hasMethodAnnotation(AdminOnly::class.java) ||
            handler.beanType.isAnnotationPresent(AdminOnly::class.java)

        if (!isAdminOnly) {
            return true
        }

        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (!metadataService.isValidAdminToken(authHeader)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return false
        }

        return true
    }
}

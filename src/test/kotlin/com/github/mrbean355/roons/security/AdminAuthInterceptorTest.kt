package com.github.mrbean355.roons.security

import com.github.mrbean355.roons.service.MetadataService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.method.HandlerMethod

internal class AdminAuthInterceptorTest {

    private lateinit var metadataService: MetadataService
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var interceptor: AdminAuthInterceptor

    @BeforeEach
    internal fun setUp() {
        metadataService = mockk()
        request = mockk()
        response = mockk(relaxed = true)
        interceptor = AdminAuthInterceptor(metadataService)
    }

    @Test
    internal fun testPreHandle_NonHandlerMethod_ReturnsTrue() {
        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        verify(inverse = true) { response.sendError(any()) }
    }

    @Test
    internal fun testPreHandle_UnsecuredEndpoint_ReturnsTrue() {
        val handler = HandlerMethod(UnsecuredController(), UnsecuredController::class.java.getMethod("openEndpoint"))

        val result = interceptor.preHandle(request, response, handler)

        assertTrue(result)
        verify(inverse = true) { response.sendError(any()) }
    }

    @Test
    internal fun testPreHandle_MethodSecured_ValidToken_ReturnsTrue() {
        val handler = HandlerMethod(MethodSecuredController(), MethodSecuredController::class.java.getMethod("securedEndpoint"))
        every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns "Bearer valid"
        every { metadataService.isValidAdminToken("Bearer valid") } returns true

        val result = interceptor.preHandle(request, response, handler)

        assertTrue(result)
        verify(inverse = true) { response.sendError(any()) }
    }

    @Test
    internal fun testPreHandle_MethodSecured_InvalidToken_Sends401AndReturnsFalse() {
        val handler = HandlerMethod(MethodSecuredController(), MethodSecuredController::class.java.getMethod("securedEndpoint"))
        every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns "Bearer invalid"
        every { metadataService.isValidAdminToken("Bearer invalid") } returns false

        val result = interceptor.preHandle(request, response, handler)

        assertFalse(result)
        verify { response.sendError(HttpServletResponse.SC_UNAUTHORIZED) }
    }

    @Test
    internal fun testPreHandle_MethodSecured_NullHeader_Sends401AndReturnsFalse() {
        val handler = HandlerMethod(MethodSecuredController(), MethodSecuredController::class.java.getMethod("securedEndpoint"))
        every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns null
        every { metadataService.isValidAdminToken(null) } returns false

        val result = interceptor.preHandle(request, response, handler)

        assertFalse(result)
        verify { response.sendError(HttpServletResponse.SC_UNAUTHORIZED) }
    }

    @Test
    internal fun testPreHandle_ClassSecured_ValidToken_ReturnsTrue() {
        val handler = HandlerMethod(ClassSecuredController(), ClassSecuredController::class.java.getMethod("securedEndpoint"))
        every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns "Bearer valid"
        every { metadataService.isValidAdminToken("Bearer valid") } returns true

        val result = interceptor.preHandle(request, response, handler)

        assertTrue(result)
        verify(inverse = true) { response.sendError(any()) }
    }

    @Test
    internal fun testPreHandle_ClassSecured_InvalidToken_Sends401AndReturnsFalse() {
        val handler = HandlerMethod(ClassSecuredController(), ClassSecuredController::class.java.getMethod("securedEndpoint"))
        every { request.getHeader(HttpHeaders.AUTHORIZATION) } returns "Bearer invalid"
        every { metadataService.isValidAdminToken("Bearer invalid") } returns false

        val result = interceptor.preHandle(request, response, handler)

        assertFalse(result)
        verify { response.sendError(HttpServletResponse.SC_UNAUTHORIZED) }
    }
}

private class UnsecuredController {
    fun openEndpoint() {}
}

private class MethodSecuredController {
    @AdminOnly
    fun securedEndpoint() {}
}

@AdminOnly
private class ClassSecuredController {
    fun securedEndpoint() {}
}

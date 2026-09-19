package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.telegram.TelegramNotifier
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.sql.SQLTransientConnectionException

internal class GlobalExceptionHandlerTest {

    @MockK(relaxed = true)
    private lateinit var telegramNotifier: TelegramNotifier

    @MockK(relaxed = true)
    private lateinit var logger: Logger

    @MockK(relaxed = true)
    private lateinit var request: HttpServletRequest

    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        handler = GlobalExceptionHandler(telegramNotifier, logger)
    }

    @Test
    internal fun testGenericException_Sends500Notification() {
        val exception = RuntimeException("Boom!")

        val response = handler.handleGenericException(request, exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        val slot = slot<String>()
        verify { telegramNotifier.sendPrivateMessage(capture(slot)) }
        assertTrue(slot.captured.contains("500 Internal Server Error"))
        assertTrue(slot.captured.contains("Boom!"))
    }

    @Test
    internal fun testDatabasePoolExhausted_SQLTransientConnectionException_SendsPoolExhaustedNotification() {
        val rootCause = SQLTransientConnectionException("Connection is not available, request timed out after 30000ms.")
        val exception = RuntimeException("Data access error", rootCause)

        val response = handler.handleGenericException(request, exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        val slot = slot<String>()
        verify { telegramNotifier.sendPrivateMessage(capture(slot)) }
        assertTrue(slot.captured.contains("Database Connection Pool Exhausted"))
        assertTrue(slot.captured.contains("Data access error"))
    }

    @Test
    internal fun testResponseStatusException_ClientError_DoesNotSendNotification() {
        val exception = ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")

        val response = handler.handleResponseStatusException(request, exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        verify(inverse = true) { telegramNotifier.sendPrivateMessage(any()) }
    }

    @Test
    internal fun testResponseStatusException_ServerError_SendsNotification() {
        val exception = ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream died")

        val response = handler.handleResponseStatusException(request, exception)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        val slot = slot<String>()
        verify { telegramNotifier.sendPrivateMessage(capture(slot)) }
        assertTrue(slot.captured.contains("500 Internal Server Error"))
        assertTrue(slot.captured.contains("Upstream died"))
    }
}

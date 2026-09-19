package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.telegram.TelegramNotifier
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.sql.SQLTransientConnectionException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger
) {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        request: HttpServletRequest,
        e: ResponseStatusException
    ): ResponseEntity<Map<String, String?>> {
        if (e.statusCode.is5xxServerError) {
            logger.error("Server error processing ${request.method} ${request.requestURI}", e)
            notifyServerError(request, e)
        }
        return ResponseEntity.status(e.statusCode).body(mapOf("error" to e.reason))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        request: HttpServletRequest,
        e: Exception
    ): ResponseEntity<Map<String, String?>> {
        logger.error("Unhandled exception processing ${request.method} ${request.requestURI}", e)

        if (isDatabasePoolExhausted(e)) {
            telegramNotifier.sendPrivateMessage(
                """
                🚨 <b>Database Connection Pool Exhausted</b>
                Endpoint: <code>${request.method} ${request.requestURI}</code>
                Error: <code>${e.message ?: "Connection timeout"}</code>
                """.trimIndent()
            )
        } else {
            notifyServerError(request, e)
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to (e.message ?: "Internal Server Error")))
    }

    private fun notifyServerError(request: HttpServletRequest, e: Throwable) {
        val message = e.message ?: "No message provided"
        telegramNotifier.sendPrivateMessage(
            """
            🔥 <b>500 Internal Server Error</b>
            Endpoint: <code>${request.method} ${request.requestURI}</code>
            Exception: <code>${e.javaClass.simpleName}</code>
            Message: <code>$message</code>
            """.trimIndent()
        )
    }

    private fun isDatabasePoolExhausted(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is SQLTransientConnectionException) {
                return true
            }
            val msg = current.message?.lowercase().orEmpty()
            if (msg.contains("connection is not available") && msg.contains("request timed out")) {
                return true
            }
            if (current.javaClass.name.contains("CannotGetJdbcConnectionException") && msg.contains("timeout")) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

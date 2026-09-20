package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.service.MetadataService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
internal class MetadataControllerTest {
    @MockK
    private lateinit var metadataService: MetadataService

    private lateinit var controller: MetadataController

    @BeforeEach
    internal fun setUp() {
        justRun { metadataService.saveWelcomeMessage(any()) }
        controller = MetadataController(metadataService)
    }

    @Test
    internal fun testGetWelcomeMessage_NullMessage_ReturnsNotFoundResult() {
        every { metadataService.getWelcomeMessage() } returns null

        val result = controller.getWelcomeMessage()

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGetWelcomeMessage_NonNullMessage_ReturnsOkResult() {
        every { metadataService.getWelcomeMessage() } returns "hello world"

        val result = controller.getWelcomeMessage()

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals("hello world", result.body?.message)
    }

    @Test
    internal fun testPutWelcomeMessage_SavesWelcomeMessage() {
        val result = controller.putWelcomeMessage(message = "new message")

        verify { metadataService.saveWelcomeMessage("new message") }
        assertSame(HttpStatus.OK, result.statusCode)
    }
}
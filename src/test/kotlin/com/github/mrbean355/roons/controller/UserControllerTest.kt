package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.service.UserService
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
internal class UserControllerTest {
    @MockK
    private lateinit var userService: UserService
    private lateinit var controller: UserController

    @BeforeEach
    internal fun setUp() {
        controller = UserController(userService)
    }

    @Test
    internal fun testCreateId_Failure_ReturnsLoopDetectedResult() {
        every { userService.createId() } returns null

        val result = controller.createId()

        assertSame(HttpStatus.LOOP_DETECTED, result.statusCode)
    }

    @Test
    internal fun testCreateId_Success_ReturnsOkResultWithGeneratedId() {
        every { userService.createId() } returns "user-123"

        val result = controller.createId()

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals("user-123", result.body?.userId)
    }

    @Test
    internal fun testHeartbeat_UpdatesLastSeen() {
        justRun { userService.updateLastSeen("user-id") }

        controller.heartbeat("user-id")

        verify { userService.updateLastSeen("user-id") }
    }
}
package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.assertTimeIsRoughlyNow
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class UserControllerTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository
    private lateinit var controller: UserController

    @BeforeEach
    internal fun setUp() {
        every { appUserRepository.save(any()) } returns mockk()
        controller = UserController(appUserRepository)
    }

    @Test
    internal fun testCreateId_GeneratedIdAlreadyUsed_ReturnsLoopDetectedResult() {
        every { appUserRepository.countByGeneratedId(any()) } returns 1

        val result = controller.createId()

        assertSame(HttpStatus.LOOP_DETECTED, result.statusCode)
    }

    @Test
    internal fun testCreateId_GeneratedIdNotUsed_SavesUser() {
        every { appUserRepository.countByGeneratedId(any()) } returns 0

        controller.createId()

        val slot = slot<AppUser>()
        verify { appUserRepository.save(capture(slot)) }
        with(slot.captured) {
            assertEquals(0, id)
            assertNotNull(UUID.fromString(generatedId))
            assertTimeIsRoughlyNow(lastSeen?.toEpochMilli())
        }
    }

    @Test
    internal fun testCreateId_GeneratedIdNotUsed_ReturnsOkResultWithGeneratedId() {
        every { appUserRepository.countByGeneratedId(any()) } returns 0

        val result = controller.createId()

        val slot = slot<AppUser>()
        verify { appUserRepository.save(capture(slot)) }
        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(slot.captured.generatedId, result.body?.userId)
    }

    @Test
    internal fun testHeartbeat_UpdatesLastSeen() {
        mockkStatic(AppUserRepository::updateLastSeen)
        justRun { appUserRepository.updateLastSeen(any()) }

        controller.heartbeat("user-id")

        verify { appUserRepository.updateLastSeen("user-id") }
    }
}
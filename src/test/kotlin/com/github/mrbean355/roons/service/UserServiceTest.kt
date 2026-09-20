package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class UserServiceTest {

    @MockK
    private lateinit var appUserRepository: AppUserRepository

    private lateinit var service: UserService

    @BeforeEach
    internal fun setUp() {
        service = UserService(appUserRepository)
    }

    @Test
    internal fun testCreateId_SavesAndReturnsGeneratedId() {
        every { appUserRepository.countByGeneratedId(any()) } returns 0
        val slot = slot<AppUser>()
        every { appUserRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.createId()

        assertNotNull(result)
        assertEquals(result, slot.captured.generatedId)
    }

    @Test
    internal fun testCreateId_Collides10Times_ReturnsNull() {
        every { appUserRepository.countByGeneratedId(any()) } returns 1

        val result = service.createId()

        assertNull(result)
        verify(exactly = 0) { appUserRepository.save(any()) }
    }

    @Test
    internal fun testUpdateLastSeen_CallsExtension() {
        mockkStatic(AppUserRepository::updateLastSeen)
        justRun { appUserRepository.updateLastSeen("user1") }

        service.updateLastSeen("user1")

        verify { appUserRepository.updateLastSeen("user1") }
    }

    @Test
    internal fun testExists_UserFound_ReturnsTrue() {
        every { appUserRepository.findByGeneratedId("user1") } returns AppUser(1, "user1", Instant.now())

        assertTrue(service.exists("user1"))
    }

    @Test
    internal fun testExists_UserNotFound_ReturnsFalse() {
        every { appUserRepository.findByGeneratedId("user1") } returns null

        assertFalse(service.exists("user1"))
    }

    @Test
    internal fun testCountRecentUsers_DelegatesToRepository() {
        val now = Instant.now()
        every { appUserRepository.countByLastSeenAfter(now) } returns 42L

        assertEquals(42L, service.countRecentUsers(now))
    }
}

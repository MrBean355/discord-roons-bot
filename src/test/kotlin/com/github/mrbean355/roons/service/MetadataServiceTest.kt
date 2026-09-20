package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.Metadata
import com.github.mrbean355.roons.repository.MetadataRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class MetadataServiceTest {

    @MockK
    private lateinit var metadataRepository: MetadataRepository

    private lateinit var service: MetadataService

    @BeforeEach
    internal fun setUp() {
        service = MetadataService(metadataRepository)
    }

    @Test
    internal fun testGetWelcomeMessage_NullValue_ReturnsNull() {
        every { metadataRepository.findByKey("app_welcome_message") } returns null

        assertNull(service.getWelcomeMessage())
    }

    @Test
    internal fun testGetWelcomeMessage_NonNullValue_ReturnsValue() {
        every { metadataRepository.findByKey("app_welcome_message") } returns Metadata("app_welcome_message", "Welcome!")

        assertEquals("Welcome!", service.getWelcomeMessage())
    }

    @Test
    internal fun testSaveWelcomeMessage_EntityNotFound_CreatesNewEntity() {
        every { metadataRepository.findByKey("app_welcome_message") } returns null
        every { metadataRepository.save(any()) } returns mockk()

        service.saveWelcomeMessage("Game on")

        verify { metadataRepository.save(Metadata("app_welcome_message", "Game on")) }
    }

    @Test
    internal fun testSaveWelcomeMessage_EntityFound_UpdatesExistingEntity() {
        every { metadataRepository.findByKey("app_welcome_message") } returns Metadata("app_welcome_message", "Old")
        every { metadataRepository.save(any()) } returns mockk()

        service.saveWelcomeMessage("New")

        verify { metadataRepository.save(Metadata("app_welcome_message", "New")) }
    }

    @Test
    internal fun testIsValidAdminToken_ConfiguredTokenNull_ReturnsFalse() {
        every { metadataRepository.findByKey("admin_token") } returns null

        assertFalse(service.isValidAdminToken("Bearer 123"))
    }

    @Test
    internal fun testIsValidAdminToken_NullHeader_ReturnsFalse() {
        every { metadataRepository.findByKey("admin_token") } returns Metadata("admin_token", "123")

        assertFalse(service.isValidAdminToken(null))
    }

    @Test
    internal fun testIsValidAdminToken_MatchingBearerToken_ReturnsTrue() {
        every { metadataRepository.findByKey("admin_token") } returns Metadata("admin_token", "123")

        assertTrue(service.isValidAdminToken("Bearer 123"))
    }

    @Test
    internal fun testIsValidAdminToken_MatchingRawToken_ReturnsTrue() {
        every { metadataRepository.findByKey("admin_token") } returns Metadata("admin_token", "123")

        assertTrue(service.isValidAdminToken("123"))
    }

    @Test
    internal fun testIsValidAdminToken_MismatchedToken_ReturnsFalse() {
        every { metadataRepository.findByKey("admin_token") } returns Metadata("admin_token", "123")

        assertFalse(service.isValidAdminToken("Bearer 456"))
    }

    @Test
    internal fun testHasAdminToken_TokenPresent_ReturnsTrue() {
        every { metadataRepository.findByKey("admin_token") } returns Metadata("admin_token", "token")

        assertTrue(service.hasAdminToken())
    }

    @Test
    internal fun testHasAdminToken_TokenMissing_ReturnsFalse() {
        every { metadataRepository.findByKey("admin_token") } returns null

        assertFalse(service.hasAdminToken())
    }
}

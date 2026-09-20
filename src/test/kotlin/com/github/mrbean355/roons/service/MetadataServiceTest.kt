package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.repository.getWelcomeMessage
import com.github.mrbean355.roons.repository.isValidAdminToken
import com.github.mrbean355.roons.repository.saveWelcomeMessage
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        mockkStatic(MetadataRepository::getWelcomeMessage)
        mockkStatic(MetadataRepository::saveWelcomeMessage)
        mockkStatic(MetadataRepository::isValidAdminToken)
        service = MetadataService(metadataRepository)
    }

    @Test
    internal fun testGetWelcomeMessage_DelegatesToRepository() {
        every { metadataRepository.getWelcomeMessage() } returns "Welcome!"

        assertEquals("Welcome!", service.getWelcomeMessage())
    }

    @Test
    internal fun testSaveWelcomeMessage_DelegatesToRepository() {
        justRun { metadataRepository.saveWelcomeMessage("New message") }

        service.saveWelcomeMessage("New message")

        verify { metadataRepository.saveWelcomeMessage("New message") }
    }

    @Test
    internal fun testIsValidAdminToken_DelegatesToRepository() {
        every { metadataRepository.isValidAdminToken("Bearer 123") } returns true

        assertTrue(service.isValidAdminToken("Bearer 123"))
    }

    @Test
    internal fun testHasAdminToken_TokenPresent_ReturnsTrue() {
        every { metadataRepository.adminToken } returns "token"

        assertTrue(service.hasAdminToken())
    }

    @Test
    internal fun testHasAdminToken_TokenMissing_ReturnsFalse() {
        every { metadataRepository.adminToken } returns null

        assertFalse(service.hasAdminToken())
    }
}

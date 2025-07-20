package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.Metadata
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MetadataRepositoryKtTest {
    @MockK
    private lateinit var metadataRepository: MetadataRepository

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    internal fun testGetWelcomeMessage_NullValue_ReturnsNull() {
        every { metadataRepository.findByKey("app_welcome_message") } returns null

        val result = metadataRepository.getWelcomeMessage()

        assertNull(result)
    }

    @Test
    internal fun testGetWelcomeMessage_NonNullValue_ReturnsValue() {
        every { metadataRepository.findByKey("app_welcome_message") } returns mockk {
            every { value } returns "hello world"
        }

        val result = metadataRepository.getWelcomeMessage()

        assertEquals("hello world", result)
    }

    @Test
    internal fun testSaveWelcomeMessage_EntityNotFound_CreatesNewEntity() {
        every { metadataRepository.findByKey("app_welcome_message") } returns null
        every { metadataRepository.save(any()) } returns mockk()

        metadataRepository.saveWelcomeMessage("Game on")

        verify { metadataRepository.save(Metadata("app_welcome_message", "Game on")) }
    }

    @Test
    internal fun testSaveWelcomeMessage_EntityFound_UpdatesExistingEntity() {
        every { metadataRepository.findByKey("app_welcome_message") } returns Metadata("app_welcome_message", "VIVON ZULUL")
        every { metadataRepository.save(any()) } returns mockk()

        metadataRepository.saveWelcomeMessage("Game on")

        verify { metadataRepository.save(Metadata("app_welcome_message", "Game on")) }
    }
}
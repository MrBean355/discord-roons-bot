package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.DotaMod
import com.github.mrbean355.roons.repository.DotaModRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(MockKExtension::class)
internal class ModServiceTest {

    @MockK
    private lateinit var dotaModRepository: DotaModRepository

    private lateinit var service: ModService

    @BeforeEach
    internal fun setUp() {
        service = ModService(dotaModRepository)
    }

    @Test
    internal fun testListMods_ReturnsMappedDtos() {
        every { dotaModRepository.findAll() } returns listOf(
            DotaMod("1", "Base mod", "Desc 1", 100, "hash1", "http://dl1", "http://info1"),
            DotaMod("2", "Spell mod", "Desc 2", 200, "hash2", "http://dl2", "http://info2")
        )

        val result = service.listMods()

        assertEquals(2, result.size)
        assertEquals("Base mod", result[0].name)
        assertEquals("Spell mod", result[1].name)
    }

    @Test
    internal fun testGetMod_NotFound_ReturnsNull() {
        every { dotaModRepository.findById("unknown") } returns Optional.empty()

        val result = service.getMod("unknown")

        assertNull(result)
    }

    @Test
    internal fun testGetMod_Found_ReturnsDto() {
        every { dotaModRepository.findById("1") } returns Optional.of(
            DotaMod("1", "Base mod", "Desc 1", 100, "hash1", "http://dl1", "http://info1")
        )

        val result = service.getMod("1")

        assertEquals("Base mod", result?.name)
    }

    @Test
    internal fun testUpdateMod_NotFound_ReturnsFalse() {
        every { dotaModRepository.findById("unknown") } returns Optional.empty()

        val result = service.updateMod("unknown", "new-hash", 500)

        assertFalse(result)
    }

    @Test
    internal fun testUpdateMod_Found_SavesAndReturnsTrue() {
        val existing = DotaMod("1", "Base mod", "Desc 1", 100, "hash1", "http://dl1", "http://info1")
        every { dotaModRepository.findById("1") } returns Optional.of(existing)
        every { dotaModRepository.save(any()) } answers { firstArg() }

        val result = service.updateMod("1", "new-hash", 500)

        assertTrue(result)
        verify { dotaModRepository.save(existing.copy(hash = "new-hash", size = 500)) }
    }
}

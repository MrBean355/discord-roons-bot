package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DotaModDto
import com.github.mrbean355.roons.service.ModService
import com.github.mrbean355.roons.telegram.TelegramNotifier
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
internal class ModControllerTest {
    @MockK
    private lateinit var modService: ModService

    @RelaxedMockK
    private lateinit var telegramNotifier: TelegramNotifier

    @MockK
    private lateinit var cacheManager: CacheManager

    @RelaxedMockK
    private lateinit var modCache: Cache
    private lateinit var controller: ModController

    @BeforeEach
    internal fun setUp() {
        every { cacheManager.getCache("dota_mod_cache") } returns modCache
        controller = ModController(modService, telegramNotifier, cacheManager)
    }

    @Test
    internal fun testListMods_FetchesFromService() {
        every { modService.listMods() } returns listOf(
            DotaModDto("1", "Base mod", "Lots of stuff", 123, "abc-123", "mods://base", "github://base"),
            DotaModDto("2", "Custom spell sounds", "Different spell sounds", 456, "def-456", "mods://sounds", "github://sounds")
        )

        val result = controller.listMods()

        assertEquals(2, result.size)
        assertEquals(DotaModDto("1", "Base mod", "Lots of stuff", 123, "abc-123", "mods://base", "github://base"), result[0])
        assertEquals(DotaModDto("2", "Custom spell sounds", "Different spell sounds", 456, "def-456", "mods://sounds", "github://sounds"), result[1])
    }

    @Test
    internal fun testGetMod_ModNotFound_ReturnsNotFoundResult() {
        every { modService.getMod("base-mod") } returns null

        val result = controller.getMod("base-mod")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGetMod_ModFound_ReturnsOkResultWithModInfo() {
        val dto = DotaModDto("1", "Base mod", "Lots of stuff", 123, "abc-123", "mods://base", "github://base")
        every { modService.getMod("base-mod") } returns dto

        val result = controller.getMod("base-mod")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(dto, result.body)
    }

    @Test
    internal fun testPatchMod_ModNotFound_ReturnsNotFoundResult() {
        every { modService.updateMod("1", "", 0) } returns false

        val result = controller.patchMod("1", "", 0, "Mod updated")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testPatchMod_ModFound_SavesModWithUpdatedSizeAndHash() {
        every { modService.updateMod("1", "new-hash", 999) } returns true

        controller.patchMod("1", "new-hash", 999, "Mod updated")

        verify { modService.updateMod("1", "new-hash", 999) }
    }

    @Test
    internal fun testPatchMod_ModFound_ClearsCache() {
        every { modService.updateMod("1", "new-hash", 999) } returns true

        controller.patchMod("1", "new-hash", 999, "Mod updated")

        verify {
            cacheManager.getCache("dota_mod_cache")
            modCache.clear()
        }
    }

    @Test
    internal fun testPatchMod_ModFound_CacheNotFound_NoExceptionThrown() {
        every { modService.updateMod("1", "new-hash", 999) } returns true
        every { cacheManager.getCache("dota_mod_cache") } returns null

        controller.patchMod("1", "new-hash", 999, "Mod updated")

        verify { cacheManager.getCache("dota_mod_cache") }
    }

    @Test
    internal fun testPatchMod_NullMessage_DoesNotSendTelegramChannelMessage() {
        every { modService.updateMod("1", "new-hash", 999) } returns true

        controller.patchMod("1", "new-hash", 999, null)

        verify(inverse = true) { telegramNotifier.sendChannelMessage(any()) }
    }

    @Test
    internal fun testPatchMod_NonNullMessage_SendsTelegramChannelMessage() {
        every { modService.updateMod("1", "new-hash", 999) } returns true

        controller.patchMod("1", "new-hash", 999, "Mod updated")

        verify { telegramNotifier.sendChannelMessage("Mod updated") }
    }

    @Test
    internal fun testPatchMod_ModFound_ReturnsOkResult() {
        every { modService.updateMod("1", "new-hash", 999) } returns true

        val result = controller.patchMod("1", "new-hash", 999, "Mod updated")

        assertSame(HttpStatus.OK, result.statusCode)
    }

    @Test
    internal fun testRefreshMods_ClearsCache() {
        controller.refreshMods()

        verify {
            cacheManager.getCache("dota_mod_cache")
            modCache.clear()
        }
    }

    @Test
    internal fun testRefreshMods_CacheNotFound_NoExceptionThrown() {
        every { cacheManager.getCache("dota_mod_cache") } returns null

        controller.refreshMods()

        verify {
            cacheManager.getCache("dota_mod_cache")
        }
    }

    @Test
    internal fun testRefreshMods_ReturnsOkResult() {
        val result = controller.refreshMods()

        assertSame(HttpStatus.OK, result.statusCode)
    }
}
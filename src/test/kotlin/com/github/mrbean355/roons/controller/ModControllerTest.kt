/*
 * Copyright 2022 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DotaMod
import com.github.mrbean355.roons.DotaModDto
import com.github.mrbean355.roons.repository.DotaModRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.repository.takeStartupMessage
import com.github.mrbean355.roons.telegram.TelegramNotifier
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import java.util.Optional

@ExtendWith(MockKExtension::class)
internal class ModControllerTest {
    @MockK
    private lateinit var dotaModRepository: DotaModRepository

    @MockK
    private lateinit var metadataRepository: MetadataRepository

    @RelaxedMockK
    private lateinit var telegramNotifier: TelegramNotifier

    @MockK
    private lateinit var cacheManager: CacheManager

    @RelaxedMockK
    private lateinit var modCache: Cache
    private lateinit var controller: ModController

    @BeforeEach
    internal fun setUp() {
        mockkStatic(MetadataRepository::takeStartupMessage)
        every { dotaModRepository.save(any()) } returns mockk()
        every { metadataRepository.adminToken } returns "12345"
        every { cacheManager.getCache("dota_mod_cache") } returns modCache
        controller = ModController(dotaModRepository, metadataRepository, telegramNotifier, cacheManager)
    }

    @Test
    internal fun testListMods_FetchesFromRepo() {
        every { dotaModRepository.findAll() } returns listOf(
            DotaMod("1", "Base mod", "Lots of stuff", 123, "abc-123", "mods://base", "github://base"),
            DotaMod("2", "Custom spell sounds", "Different spell sounds", 456, "def-456", "mods://sounds", "github://sounds")
        )

        val result = controller.listMods()

        assertEquals(2, result.size)
        assertEquals(DotaModDto("1", "Base mod", "Lots of stuff", 123, "abc-123", "mods://base", "github://base"), result[0])
        assertEquals(DotaModDto("2", "Custom spell sounds", "Different spell sounds", 456, "def-456", "mods://sounds", "github://sounds"), result[1])
    }

    @Test
    internal fun testGetMod_ModNotFound_ReturnsNotFoundResult() {
        every { dotaModRepository.findById("base-mod") } returns Optional.empty()

        val result = controller.getMod("base-mod")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGetMod_ModFound_ReturnsOkResultWithModInfo() {
        every { dotaModRepository.findById("base-mod") } returns Optional.of(createMod())

        val result = controller.getMod("base-mod")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(DotaModDto("1", "Base mod", "Lots of stuff", 123, "abc-123", "mods://base", "github://base"), result.body)
    }

    @Test
    internal fun testPatchMod_IncorrectToken_ReturnsUnauthorized() {
        val result = controller.patchMod("", "", 0, "67890", "Mod updated")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testPatchMod_ModNotFound_ReturnsNotFoundResult() {
        every { dotaModRepository.findById("1") } returns Optional.empty()

        val result = controller.patchMod("1", "", 0, "12345", "Mod updated")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testPatchMod_ModFound_SavesModWithUpdatedSizeAndHash() {
        every { dotaModRepository.findById("1") } returns Optional.of(createMod())

        controller.patchMod("1", "new-hash", 999, "12345", "Mod updated")

        verify { dotaModRepository.save(DotaMod("1", "Base mod", "Lots of stuff", 999, "new-hash", "mods://base", "github://base")) }
    }

    @Test
    internal fun testPatchMod_ModFound_ClearsCache() {
        every { dotaModRepository.findById("1") } returns Optional.of(createMod())

        controller.patchMod("1", "new-hash", 999, "12345", "Mod updated")

        verify {
            cacheManager.getCache("dota_mod_cache")
            modCache.clear()
        }
    }

    @Test
    internal fun testPatchMod_ModFound_CacheNotFound_NoExceptionThrown() {
        every { dotaModRepository.findById("1") } returns Optional.of(createMod())
        every { cacheManager.getCache("dota_mod_cache") } returns null

        controller.patchMod("1", "new-hash", 999, "12345", "Mod updated")

        verify { cacheManager.getCache("dota_mod_cache") }
    }

    @Test
    internal fun testPatchMod_NullMessage_DoesNotSendTelegramChannelMessage() {
        every { dotaModRepository.findById("1") } returns Optional.of(createMod("Custom spell sounds"))

        controller.patchMod("1", "new-hash", 999, "12345", null)

        verify(inverse = true) { telegramNotifier.sendChannelMessage(any()) }
    }

    @Test
    internal fun testPatchMod_NonNullMessage_SendsTelegramChannelMessage() {
        every { dotaModRepository.findById("1") } returns Optional.of(createMod())

        controller.patchMod("1", "new-hash", 999, "12345", "Mod updated")

        verify { telegramNotifier.sendChannelMessage("Mod updated") }
    }

    @Test
    internal fun testPatchMod_ModFound_ReturnsOkResult() {
        every { dotaModRepository.findById("1") } returns Optional.of(createMod())

        val result = controller.patchMod("1", "new-hash", 999, "12345", "Mod updated")

        assertSame(HttpStatus.OK, result.statusCode)
    }

    @Test
    internal fun testRefreshMods_IncorrectToken_ReturnsUnauthorizedResult() {
        val result = controller.refreshMods("67890")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testRefreshMods_CorrectToken_ClearsCache() {
        controller.refreshMods("12345")

        verify {
            cacheManager.getCache("dota_mod_cache")
            modCache.clear()
        }
    }

    @Test
    internal fun testRefreshMods_CacheNotFound_NoExceptionThrown() {
        every { cacheManager.getCache("dota_mod_cache") } returns null

        controller.refreshMods("12345")

        verify {
            cacheManager.getCache("dota_mod_cache")
        }
    }

    @Test
    internal fun testRefreshMods_CorrectToken_ReturnsOkResult() {
        val result = controller.refreshMods("12345")

        assertSame(HttpStatus.OK, result.statusCode)
    }

    private fun createMod(name: String = "Base mod"): DotaMod =
        DotaMod("1", name, "Lots of stuff", 123, "abc-123", "mods://base", "github://base")
}
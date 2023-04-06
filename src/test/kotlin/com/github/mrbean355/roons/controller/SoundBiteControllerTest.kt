/*
 * Copyright 2023 Michael Johnston
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

import com.github.mrbean355.roons.PlaySound
import com.github.mrbean355.roons.discord.SoundStore
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpStatus
import java.io.File

@ExtendWith(MockKExtension::class)
internal class SoundBiteControllerTest {
    @MockK
    private lateinit var soundStore: SoundStore
    private lateinit var controller: SoundBiteController

    @BeforeEach
    internal fun setUp() {
        controller = SoundBiteController(soundStore)
    }

    @Test
    @Suppress("DEPRECATION")
    internal fun testListV2_FetchesFromStoreAndTransformsResult() {
        val sounds = getMockSounds()
        every { soundStore.listAll() } returns sounds

        val result = controller.listV2()

        assertEquals(sounds.size, result.size)
        sounds.forEach {
            assertTrue(it.name in result)
            assertEquals(it.checksum, result.getValue(it.name))
        }
    }

    @Test
    internal fun testListV3_FetchesFromStore() {
        val sounds = getMockSounds()
        every { soundStore.listAll() } returns sounds

        val result = controller.listV3()

        assertSame(sounds, result)
    }

    @Test
    internal fun testGet_FileNotFound_ReturnsNotFoundResult() {
        every { soundStore.getFile("roons.mp3") } returns null

        val result = controller.get("roons.mp3")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGet_FileFound_ReturnsOkResultWithHeaders() {
        val soundFile = File("src/test/resources/roons.mp3")
        every { soundStore.getFile("roons.mp3") } returns soundFile

        val result = controller.get("roons.mp3")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals("attachment; filename=\"roons.mp3\"", result.headers.getFirst("Content-Disposition"))
        assertTrue(result.body is UrlResource)
        assertEquals(soundFile.absolutePath, (result.body as UrlResource).file.absolutePath)
    }

    private fun getMockSounds(): Collection<PlaySound> {
        return listOf(
            PlaySound("roons.mp3", "3f0c5367ee", "Bulldog's voice"),
            PlaySound("eel.mp3", "8332e735d8", "Bulldog's voice"),
            PlaySound("weed.mp3", "101d75dad9", "Songs"),
            PlaySound("vivon.mp3", "b4d443041a", "Ugandan"),
        )
    }
}
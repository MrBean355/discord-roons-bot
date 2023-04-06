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
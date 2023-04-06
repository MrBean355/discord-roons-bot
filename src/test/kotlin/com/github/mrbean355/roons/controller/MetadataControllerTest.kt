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

import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.repository.getWelcomeMessage
import com.github.mrbean355.roons.repository.saveWelcomeMessage
import com.github.mrbean355.roons.repository.takeStartupMessage
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
internal class MetadataControllerTest {
    @MockK
    private lateinit var metadataRepository: MetadataRepository

    @RelaxedMockK
    private lateinit var applicationContext: ConfigurableApplicationContext

    @RelaxedMockK
    private lateinit var discordBot: DiscordBot

    @MockK
    private lateinit var cacheManager: CacheManager

    @RelaxedMockK
    private lateinit var welcomeMessageCache: Cache
    private lateinit var controller: MetadataController

    @BeforeEach
    internal fun setUp() {
        mockkStatic(MetadataRepository::takeStartupMessage)
        every { metadataRepository.adminToken } returns "12345"
        every { cacheManager.getCache("welcome_message_cache") } returns welcomeMessageCache
        justRun { metadataRepository.saveWelcomeMessage(any()) }
        controller = MetadataController(metadataRepository, applicationContext, discordBot, cacheManager)
    }

    @Test
    internal fun testGetWelcomeMessage_NullMessage_ReturnsNotFoundResult() {
        every { metadataRepository.getWelcomeMessage() } returns null

        val result = controller.getWelcomeMessage()

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGetWelcomeMessage_NonNullMessage_ReturnsOkResult() {
        every { metadataRepository.getWelcomeMessage() } returns "hello world"

        val result = controller.getWelcomeMessage()

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals("hello world", result.body?.message)
    }

    @Test
    internal fun testPutWelcomeMessage_IncorrectToken_ReturnsUnauthorizedResult() {
        val result = controller.putWelcomeMessage("67890", "")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testPutWelcomeMessage_CorrectToken_SavesWelcomeMessage() {
        val result = controller.putWelcomeMessage("12345", "new message")

        verifyOrder {
            metadataRepository.saveWelcomeMessage("new message")
            cacheManager.getCache("welcome_message_cache")
            welcomeMessageCache.clear()
        }
        assertSame(HttpStatus.OK, result.statusCode)
    }

    @Test
    internal fun testShutdown_AdminTokenNotFound_ReturnsInternalServerErrorResult() {
        every { metadataRepository.adminToken } returns null

        val result = controller.shutdown("")

        assertSame(HttpStatus.INTERNAL_SERVER_ERROR, result.statusCode)
    }

    @Test
    internal fun testShutdown_IncorrectToken_ReturnsUnauthorizedResult() {
        val result = controller.shutdown("67890")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testShutdown_CorrectToken_ShutsDownApplication() {
        controller.shutdown("12345")

        verify {
            discordBot.shutdown()
            applicationContext.close()
        }
    }

    @Test
    internal fun testShutdown_WrongTypeApplicationContext_NoExceptionThrown() {
        controller = MetadataController(metadataRepository, mockk(), discordBot, cacheManager)

        controller.shutdown("12345")

        verify { discordBot.shutdown() }
    }

    @Test
    internal fun testShutdown_CorrectToken_ReturnsOkResult() {
        val result = controller.shutdown("12345")

        assertSame(HttpStatus.OK, result.statusCode)
    }
}
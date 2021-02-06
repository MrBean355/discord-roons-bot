/*
 * Copyright 2021 Michael Johnston
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
import com.github.mrbean355.roons.repository.takeStartupMessage
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
    private lateinit var controller: MetadataController

    @BeforeEach
    internal fun setUp() {
        mockkStatic(MetadataRepository::takeStartupMessage)
        every { metadataRepository.adminToken } returns "12345"
        controller = MetadataController(metadataRepository, applicationContext, discordBot)
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
        controller = MetadataController(metadataRepository, mockk(), discordBot)

        controller.shutdown("12345")

        verify { discordBot.shutdown() }
    }

    @Test
    internal fun testShutdown_CorrectToken_ReturnsOkResult() {
        val result = controller.shutdown("12345")

        assertSame(HttpStatus.OK, result.statusCode)
    }
}
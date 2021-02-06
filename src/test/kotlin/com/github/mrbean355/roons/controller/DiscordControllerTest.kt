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

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.PlaySoundRequest
import com.github.mrbean355.roons.PlaySoundsRequest
import com.github.mrbean355.roons.SingleSound
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.discord.SoundStore
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
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
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
internal class DiscordControllerTest {
    @MockK
    private lateinit var discordBot: DiscordBot

    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var discordBotUserRepository: DiscordBotUserRepository

    @MockK
    private lateinit var soundStore: SoundStore
    private lateinit var controller: DiscordController

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(AppUserRepository::updateLastSeen)
        justRun { appUserRepository.updateLastSeen(any()) }
        controller = DiscordController(discordBot, appUserRepository, discordBotUserRepository, soundStore)
    }

    @Test
    internal fun testLookupToken_UserNotFound_ReturnsNotFoundResult() {
        every { discordBotUserRepository.findOneByToken("abc123") } returns null

        val result = controller.lookupToken("abc123")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testLookupToken_GuildNotFound_ReturnsNotFoundResult() {
        every { discordBotUserRepository.findOneByToken("abc123") } returns mockk {
            every { guildId } returns "guild-id"
        }
        every { discordBot.getGuildById("guild-id") } returns null

        val result = controller.lookupToken("abc123")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testLookupToken_UserAndGuildFound_ReturnsOkResultWithGuildName() {
        every { discordBotUserRepository.findOneByToken("abc123") } returns mockk {
            every { guildId } returns "guild-id"
        }
        every { discordBot.getGuildById("guild-id") } returns mockk {
            every { name } returns "Mr Bean Dota"
        }

        val result = controller.lookupToken("abc123")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals("Mr Bean Dota", result.body)
    }

    @Test
    internal fun testPlaySound_UpdatesUserLastSeen() {
        controller.playSound(mockRequest(token = ""))

        verify { appUserRepository.updateLastSeen("user-id") }
    }

    @Test
    internal fun testPlaySound_BlankToken_ReturnsUnauthorizedResult() {
        val result = controller.playSound(mockRequest(token = ""))

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testPlaySound_UserNotFound_ReturnsUnauthorizedResult() {
        every { discordBotUserRepository.findOneByToken("token") } returns null

        val result = controller.playSound(mockRequest())

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testPlaySound_SoundNotFound_ReturnsBadRequestResult() {
        every { discordBotUserRepository.findOneByToken("token") } returns mockk()
        every { soundStore.soundExists("roons.mp3") } returns false

        val result = controller.playSound(mockRequest())

        assertSame(HttpStatus.BAD_REQUEST, result.statusCode)
    }

    @Test
    internal fun testPlaySound_SoundFound_PlaysSound(
        @MockK discordBotUser: DiscordBotUser
    ) {
        every { discordBotUserRepository.findOneByToken("token") } returns discordBotUser
        every { soundStore.soundExists("roons.mp3") } returns true
        every { discordBot.playSound(any(), any(), any(), any()) } returns false

        controller.playSound(mockRequest())

        verify { discordBot.playSound(discordBotUser, "roons.mp3", 85, 125) }
    }

    @Test
    internal fun testPlaySound_SoundFound_MissingVolumeAndRate_PlaysSoundWithDefaults(
        @MockK discordBotUser: DiscordBotUser
    ) {
        every { discordBotUserRepository.findOneByToken("token") } returns discordBotUser
        every { soundStore.soundExists("roons.mp3") } returns true
        every { discordBot.playSound(any(), any(), any(), any()) } returns false

        controller.playSound(mockEmptyRequest())

        verify { discordBot.playSound(discordBotUser, "roons.mp3", 100, 100) }
    }

    @Test
    internal fun testPlaySound_SoundFailedToPlay_ReturnsPreconditionFailedResult() {
        every { discordBotUserRepository.findOneByToken("token") } returns mockk()
        every { soundStore.soundExists("roons.mp3") } returns true
        every { discordBot.playSound(any(), any(), any(), any()) } returns false

        val result = controller.playSound(mockRequest())

        assertSame(HttpStatus.PRECONDITION_FAILED, result.statusCode)
    }

    @Test
    internal fun testPlaySound_SoundPlaysSuccessfully_ReturnsOkResult() {
        every { discordBotUserRepository.findOneByToken("token") } returns mockk()
        every { soundStore.soundExists("roons.mp3") } returns true
        every { discordBot.playSound(any(), any(), any(), any()) } returns true

        val result = controller.playSound(mockRequest())

        assertSame(HttpStatus.OK, result.statusCode)
    }

    @Test
    internal fun testPlaySounds_UpdatesUserLastSeen() {
        controller.playSounds(mockMultiRequest(token = ""))

        verify { appUserRepository.updateLastSeen("user-id") }
    }

    @Test
    internal fun testPlaySounds_BlankToken_ReturnsUnauthorizedResult() {
        val result = controller.playSounds(mockMultiRequest(token = ""))

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testPlaySounds_EmptySounds_ReturnsBadRequestResult() {
        val result = controller.playSounds(mockEmptyMultiRequest())

        assertSame(HttpStatus.BAD_REQUEST, result.statusCode)
    }

    @Test
    internal fun testPlaySounds_UserNotFound_ReturnsUnauthorizedResult() {
        every { discordBotUserRepository.findOneByToken("token") } returns null

        val result = controller.playSounds(mockMultiRequest())

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testPlaySounds_PlaysEachSound(
        @MockK discordBotUser: DiscordBotUser
    ) {
        every { discordBotUserRepository.findOneByToken("token") } returns discordBotUser
        every { discordBot.playSound(any(), any(), any(), any()) } returns false

        controller.playSounds(mockMultiRequest())

        verifyOrder {
            discordBot.playSound(discordBotUser, "heyguys.mp3", 66, 88)
            discordBot.playSound(discordBotUser, "stayhome.mp3", 110, 133)
        }
    }

    @Test
    internal fun testPlaySounds_AllSoundsPlaySuccessfully_ReturnsOkResult(
        @MockK discordBotUser: DiscordBotUser
    ) {
        every { discordBotUserRepository.findOneByToken("token") } returns discordBotUser
        every { discordBot.playSound(any(), any(), any(), any()) } returns true

        val result = controller.playSounds(mockMultiRequest())

        assertSame(HttpStatus.OK, result.statusCode)
    }

    @Test
    internal fun testPlaySounds_OneSoundPlaysSuccessfully_ReturnsOkResult(
        @MockK discordBotUser: DiscordBotUser
    ) {
        every { discordBotUserRepository.findOneByToken("token") } returns discordBotUser
        every { discordBot.playSound(any(), any(), any(), any()) }.returnsMany(true, false)

        val result = controller.playSounds(mockMultiRequest())

        assertSame(HttpStatus.OK, result.statusCode)
    }

    @Test
    internal fun testPlaySounds_AllSoundsFailToPlay_ReturnsBadRequestResult(
        @MockK discordBotUser: DiscordBotUser
    ) {
        every { discordBotUserRepository.findOneByToken("token") } returns discordBotUser
        every { discordBot.playSound(any(), any(), any(), any()) } returns false

        val result = controller.playSounds(mockMultiRequest())

        assertSame(HttpStatus.BAD_REQUEST, result.statusCode)
    }

    private fun mockRequest(token: String = "token"): PlaySoundRequest = mockk {
        every { this@mockk.userId } returns "user-id"
        every { this@mockk.token } returns token
        every { this@mockk.soundFileName } returns "roons.mp3"
        every { this@mockk.volume } returns 85
        every { this@mockk.rate } returns 125
    }

    private fun mockEmptyRequest(): PlaySoundRequest = mockk {
        every { this@mockk.userId } returns "user-id"
        every { this@mockk.token } returns "token"
        every { this@mockk.soundFileName } returns "roons.mp3"
        every { this@mockk.volume } returns null
        every { this@mockk.rate } returns null
    }

    private fun mockMultiRequest(token: String = "token"): PlaySoundsRequest = mockk {
        every { this@mockk.userId } returns "user-id"
        every { this@mockk.token } returns token
        every { this@mockk.sounds } returns listOf(
            SingleSound("heyguys.mp3", 66, 88),
            SingleSound("stayhome.mp3", 110, 133)
        )
    }

    private fun mockEmptyMultiRequest(): PlaySoundsRequest = mockk {
        every { this@mockk.userId } returns "user-id"
        every { this@mockk.token } returns "token"
        every { this@mockk.sounds } returns emptyList()
    }
}
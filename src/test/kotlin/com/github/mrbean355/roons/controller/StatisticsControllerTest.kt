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

import com.github.mrbean355.roons.DiscordServerDto
import com.github.mrbean355.roons.TestClock
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.dv8tion.jda.api.entities.Guild
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import java.util.Date

@ExtendWith(MockKExtension::class)
internal class StatisticsControllerTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var analyticsPropertyRepository: AnalyticsPropertyRepository

    @MockK
    private lateinit var metadataRepository: MetadataRepository

    @MockK
    private lateinit var discordBot: DiscordBot
    private lateinit var controller: StatisticsController

    @BeforeEach
    internal fun setUp() {
        every { metadataRepository.adminToken } returns "12345"
        controller = StatisticsController(appUserRepository, analyticsPropertyRepository, metadataRepository, discordBot, TestClock(1_000_000))
    }

    @Test
    internal fun testListProperties_WrongToken_ReturnsUnauthorized() {
        val result = controller.listProperties("1111")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testListProperties_CorrectToken_ReturnsProperties() {
        every { analyticsPropertyRepository.findDistinctProperties() } returns listOf("a", "b", "c")

        val result = controller.listProperties("12345")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(listOf("a", "b", "c"), result.body)
    }

    @Test
    internal fun testGetRecentUsers_WrongToken_ReturnsUnauthorized() {
        val result = controller.getRecentUsers("1111", 5)

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testGetRecentUsers_CorrectToken_ReturnsProperties() {
        every { appUserRepository.countByLastSeenAfter(any()) } returns 999

        val result = controller.getRecentUsers("12345", 5)

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(999, result.body ?: 0)
        val slot = slot<Date>()
        verify { appUserRepository.countByLastSeenAfter(capture(slot)) }
        assertEquals(700_000, slot.captured.time)
    }

    @Test
    internal fun testGetStatistic_WrongToken_ReturnsUnauthorized() {
        val result = controller.getStatistic("1111", "")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testGetStatistic_CorrectToken_PropertyNotFound_ReturnsNotFound() {
        every { analyticsPropertyRepository.findByProperty("abc") } returns emptyList()

        val result = controller.getStatistic("12345", "abc")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGetStatistic_CorrectToken_PropertyFound_ReturnsValueCountMap() {
        every { analyticsPropertyRepository.findByProperty("abc") } returns listOf(
            mockk { every { value } returns "one" },
            mockk { every { value } returns "two,three" },
            mockk { every { value } returns "one,two,three" },
            mockk { every { value } returns "two,four" }
        )

        val result = controller.getStatistic("12345", "abc")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(4, result.body?.size ?: 0)
        assertEquals(2, result.body?.getValue("one"))
        assertEquals(3, result.body?.getValue("two"))
        assertEquals(2, result.body?.getValue("three"))
        assertEquals(1, result.body?.getValue("four"))
    }

    @Test
    internal fun testGetDiscordServers_IncorrectToken_ReturnsUnauthorizedResponse() {
        val result = controller.getDiscordServers("67890")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testGetDiscordServers_CorrectToken_ReturnsGuildList() {
        every { discordBot.getGuilds() } returns listOf(
            mockGuild("Mr Bean Dota", 284, "Squad"),
            mockGuild("The Krappa Kleb", 74, "General"),
            mockGuild("Bruh", 10),
            mockGuild("Dungeon", 25)
        )

        val result = controller.getDiscordServers("12345")
        val body = result.body.orEmpty()

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(4, body.size)
        assertEquals(DiscordServerDto("Mr Bean Dota", 284, "Squad"), body[0])
        assertEquals(DiscordServerDto("The Krappa Kleb", 74, "General"), body[1])
        assertEquals(DiscordServerDto("Bruh", 10, null), body[2])
        assertEquals(DiscordServerDto("Dungeon", 25, null), body[3])
    }

    private fun mockGuild(
        guildName: String,
        guildMembers: Int,
        guildVoiceChannel: String? = null
    ): Guild = mockk {
        every { name } returns guildName
        every { memberCount } returns guildMembers
        every { audioManager } returns mockk {
            if (guildVoiceChannel != null) {
                every { connectedChannel } returns mockk {
                    every { name } returns guildVoiceChannel
                }
            } else {
                every { connectedChannel } returns null
            }
        }
    }
}
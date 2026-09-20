package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DiscordServerDto
import com.github.mrbean355.roons.TestClock
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.service.AnalyticsService
import com.github.mrbean355.roons.service.UserService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class StatisticsControllerTest {
    @MockK
    private lateinit var userService: UserService

    @MockK
    private lateinit var analyticsService: AnalyticsService

    @MockK
    private lateinit var discordBot: DiscordBot
    private lateinit var controller: StatisticsController

    @BeforeEach
    internal fun setUp() {
        controller = StatisticsController(userService, analyticsService, discordBot, TestClock(1_000_000))
    }

    @Test
    internal fun testGetHealth_ReturnsSystemHealth() {
        every { discordBot.getGatewayStatus() } returns JDA.Status.CONNECTED
        every { discordBot.getGatewayPing() } returns 42L

        val result = controller.getHealth()

        assertSame(HttpStatus.OK, result.statusCode)
        val body = result.body
        assertNotNull(body)
        assertEquals("CONNECTED", body?.discordStatus)
        assertEquals(42L, body?.discordPing)
    }

    @Test
    internal fun testListProperties_ReturnsProperties() {
        every { analyticsService.findDistinctProperties() } returns listOf("a", "b", "c")

        val result = controller.listProperties()

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(listOf("a", "b", "c"), result.body)
    }

    @Test
    internal fun testGetRecentUsers_ReturnsUserCount() {
        every { userService.countRecentUsers(any()) } returns 999

        val result = controller.getRecentUsers(5)

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(999, result.body ?: 0)
        val slot = slot<Instant>()
        verify { userService.countRecentUsers(capture(slot)) }
        assertEquals(700_000, slot.captured.toEpochMilli())
    }

    @Test
    internal fun testGetStatistic_PropertyNotFound_ReturnsNotFound() {
        every { analyticsService.getStatistic("abc") } returns null

        val result = controller.getStatistic("abc")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGetStatistic_PropertyFound_ReturnsValueCountMap() {
        val stats = mapOf("one" to 2, "two" to 3, "three" to 2, "four" to 1)
        every { analyticsService.getStatistic("abc") } returns stats

        val result = controller.getStatistic("abc")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(stats, result.body)
    }

    @Test
    internal fun testGetDiscordServers_ReturnsGuildList() {
        every { discordBot.getGuilds() } returns listOf(
            mockGuild("Mr Bean Dota", 284, "Squad"),
            mockGuild("The Krappa Kleb", 74, "General"),
            mockGuild("Bruh", 10),
            mockGuild("Dungeon", 25)
        )

        val result = controller.getDiscordServers()
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
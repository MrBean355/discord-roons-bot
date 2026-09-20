package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.discord.audio.DEFAULT_VOLUME
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class DiscordBotServiceTest {

    @MockK
    private lateinit var discordBotUserRepository: DiscordBotUserRepository

    @MockK
    private lateinit var discordBotSettingsRepository: DiscordBotSettingsRepository

    private lateinit var service: DiscordBotService

    @BeforeEach
    internal fun setUp() {
        service = DiscordBotService(discordBotUserRepository, discordBotSettingsRepository)
    }

    @Test
    internal fun testFindUserByToken_Found_ReturnsUser() {
        val user = DiscordBotUser(1, "user1", "guild1", "tok")
        every { discordBotUserRepository.findOneByToken("tok") } returns user

        assertEquals(user, service.findUserByToken("tok"))
    }

    @Test
    internal fun testFindUserByToken_NotFound_ReturnsNull() {
        every { discordBotUserRepository.findOneByToken("unknown") } returns null

        assertNull(service.findUserByToken("unknown"))
    }

    @Test
    internal fun testCleanUpGuild_DeletesUsersAndSettings() {
        every { discordBotUserRepository.deleteByGuildId("g1") } returns 1
        every { discordBotSettingsRepository.deleteByGuildId("g1") } returns 1

        service.cleanUpGuild("g1")

        verify {
            discordBotUserRepository.deleteByGuildId("g1")
            discordBotSettingsRepository.deleteByGuildId("g1")
        }
    }

    @Test
    internal fun testLoadSettings_SettingsExist_ReturnsExistingSettings() {
        val existing = DiscordBotSettings(1, "g1", 80, "user1", "channel1")
        every { discordBotSettingsRepository.findOneByGuildId("g1") } returns existing

        val result = service.loadSettings("g1")

        assertEquals(existing, result)
        verify(exactly = 0) { discordBotSettingsRepository.save(any()) }
    }

    @Test
    internal fun testLoadSettings_SettingsDoNotExist_CreatesAndReturnsDefaultSettings() {
        every { discordBotSettingsRepository.findOneByGuildId("g1") } returns null
        val slot = slot<DiscordBotSettings>()
        every { discordBotSettingsRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.loadSettings("g1")

        assertEquals(DiscordBotSettings(0, "g1", DEFAULT_VOLUME, null, null), result)
        assertEquals("g1", slot.captured.guildId)
        assertEquals(DEFAULT_VOLUME, slot.captured.volume)
    }

    @Test
    internal fun testSaveSettings_DelegatesToRepository() {
        val settings = DiscordBotSettings(1, "g1", 50, null, null)
        every { discordBotSettingsRepository.save(settings) } returns settings

        val result = service.saveSettings(settings)

        assertEquals(settings, result)
        verify { discordBotSettingsRepository.save(settings) }
    }

    @Test
    internal fun testGetOrCreateUserToken_UserExists_ReturnsExistingToken() {
        val existing = DiscordBotUser(1, "u1", "g1", "token123")
        every { discordBotUserRepository.findOneByDiscordUserIdAndGuildId("u1", "g1") } returns existing

        val result = service.getOrCreateUserToken("u1", "g1")

        assertEquals("token123", result)
        verify(exactly = 0) { discordBotUserRepository.save(any()) }
    }

    @Test
    internal fun testGetOrCreateUserToken_UserDoesNotExist_SavesAndReturnsNewToken() {
        every { discordBotUserRepository.findOneByDiscordUserIdAndGuildId("u1", "g1") } returns null
        val slot = slot<DiscordBotUser>()
        every { discordBotUserRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.getOrCreateUserToken("u1", "g1")

        assertEquals(slot.captured.token, result)
        assertEquals("u1", slot.captured.discordUserId)
        assertEquals("g1", slot.captured.guildId)
    }

    @Test
    internal fun testGenerateNewUserToken_UserExists_UpdatesTokenAndReturnsIt() {
        val existing = DiscordBotUser(1, "u1", "g1", "oldToken")
        every { discordBotUserRepository.findOneByDiscordUserIdAndGuildId("u1", "g1") } returns existing
        val slot = slot<DiscordBotUser>()
        every { discordBotUserRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.generateNewUserToken("u1", "g1")

        assertEquals(slot.captured.token, result)
        assertEquals("u1", slot.captured.discordUserId)
        assertEquals("g1", slot.captured.guildId)
        assert(result != "oldToken")
    }

    @Test
    internal fun testGenerateNewUserToken_UserDoesNotExist_SavesNewUserAndReturnsToken() {
        every { discordBotUserRepository.findOneByDiscordUserIdAndGuildId("u1", "g1") } returns null
        val slot = slot<DiscordBotUser>()
        every { discordBotUserRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.generateNewUserToken("u1", "g1")

        assertEquals(slot.captured.token, result)
        assertEquals("u1", slot.captured.discordUserId)
        assertEquals("g1", slot.captured.guildId)
    }
}

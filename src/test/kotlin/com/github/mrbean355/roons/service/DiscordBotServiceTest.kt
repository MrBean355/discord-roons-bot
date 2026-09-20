package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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
}

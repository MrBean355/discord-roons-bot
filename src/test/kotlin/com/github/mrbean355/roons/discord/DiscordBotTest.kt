package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.service.DiscordBotService
import com.github.mrbean355.roons.telegram.TelegramNotifier
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.managers.AudioManager
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.io.File

internal class DiscordBotTest {

    @MockK(relaxed = true)
    private lateinit var discordBotService: DiscordBotService

    @MockK(relaxed = true)
    private lateinit var soundStore: SoundStore

    @MockK(relaxed = true)
    private lateinit var telegramNotifier: TelegramNotifier

    @MockK(relaxed = true)
    private lateinit var logger: Logger

    @MockK(relaxed = true)
    private lateinit var bot: JDA

    private lateinit var discordBot: DiscordBot

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        discordBot = DiscordBot(discordBotService, soundStore, telegramNotifier, logger, bot)
    }

    @Test
    internal fun testPlaySound_GuildNotFound_ReturnsFalse() {
        every { bot.getGuildById("guild_1") } returns null

        val result = discordBot.playSound(DiscordBotUser(1, "user_1", "guild_1", "token_1"), "13.mp3", 100, 100)

        assertFalse(result)
    }

    @Test
    internal fun testPlaySound_SoundFileNotFound_ReturnsFalse() {
        val guild = mockk<Guild>(relaxed = true)
        every { bot.getGuildById("guild_1") } returns guild
        every { soundStore.getFile("unknown.mp3") } returns null

        val result = discordBot.playSound(DiscordBotUser(1, "user_1", "guild_1", "token_1"), "unknown.mp3", 100, 100)

        assertFalse(result)
    }

    @Test
    internal fun testPlaySound_NotInVoiceChannel_ReturnsFalse() {
        val guild = mockk<Guild>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { audioManager.isConnected } returns false
        every { guild.audioManager } returns audioManager
        every { bot.getGuildById("guild_1") } returns guild
        every { soundStore.getFile("13.mp3") } returns File("13.mp3")
        every { discordBotService.loadSettings("guild_1") } returns DiscordBotSettings(1, "guild_1", 100, null, null)

        val result = discordBot.playSound(DiscordBotUser(1, "user_1", "guild_1", "token_1"), "13.mp3", 100, 100)

        assertFalse(result)
    }

    @Test
    internal fun testShutdown_DisconnectsConnectedGuildsAndSavesLastChannel() {
        val guild = mockk<Guild>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { guild.id } returns "guild_1"
        every { guild.audioManager } returns audioManager
        every { audioManager.isConnected } returns true
        every { guild.selfMember.voiceState?.channel?.id } returns "channel_123"
        every { bot.guilds } returns listOf(guild)
        every { discordBotService.loadSettings("guild_1") } returns DiscordBotSettings(1, "guild_1", 100, null, null)
        every { discordBotService.saveSettings(any()) } answers { firstArg() }

        discordBot.shutdown()

        verify { bot.presence.setStatus(OnlineStatus.OFFLINE) }
        verify { discordBotService.saveSettings(match { it.lastChannel == "channel_123" }) }
        verify { audioManager.closeAudioConnection() }
    }
}

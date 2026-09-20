package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.discord.commands.BotCommand
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.service.AnalyticsService
import com.github.mrbean355.roons.service.DiscordBotService
import com.github.mrbean355.roons.telegram.TelegramNotifier
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.managers.AudioManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DiscordEventHandlerTest {

    @MockK(relaxed = true)
    private lateinit var command: BotCommand

    @MockK(relaxed = true)
    private lateinit var discordBotService: DiscordBotService

    @MockK(relaxed = true)
    private lateinit var discordBotSettingsRepository: DiscordBotSettingsRepository

    @MockK(relaxed = true)
    private lateinit var telegramNotifier: TelegramNotifier

    @MockK(relaxed = true)
    private lateinit var analyticsService: AnalyticsService

    private lateinit var discordEventHandler: DiscordEventHandler

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        every { command.name } returns "test_cmd"
        every { command.description } returns "test description"
        discordEventHandler = DiscordEventHandler(
            listOf(command),
            discordBotService,
            discordBotSettingsRepository,
            telegramNotifier,
            analyticsService,
            CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    internal fun testOnGuildVoiceUpdate_MemberIsBot_DoesNothing() {
        val event = mockk<GuildVoiceUpdateEvent>(relaxed = true)
        every { event.member.user.isBot } returns true

        discordEventHandler.onGuildVoiceUpdate(event)

        verify(exactly = 0) { discordBotSettingsRepository.findOneByGuildId(any()) }
    }

    @Test
    internal fun testOnGuildVoiceUpdate_SettingsNotFound_DoesNothing() {
        val event = mockk<GuildVoiceUpdateEvent>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { event.member.user.isBot } returns false
        every { event.guild.id } returns "guild_1"
        every { event.guild.audioManager } returns audioManager
        every { discordBotSettingsRepository.findOneByGuildId("guild_1") } returns null

        discordEventHandler.onGuildVoiceUpdate(event)

        verify(exactly = 0) { audioManager.openAudioConnection(any()) }
        verify(exactly = 0) { audioManager.closeAudioConnection() }
    }

    @Test
    internal fun testOnGuildVoiceUpdate_UserNotFollowed_DoesNothing() {
        val event = mockk<GuildVoiceUpdateEvent>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)
        every { event.member.user.isBot } returns false
        every { event.member.id } returns "user_1"
        every { event.guild.id } returns "guild_1"
        every { event.guild.audioManager } returns audioManager
        every { discordBotSettingsRepository.findOneByGuildId("guild_1") } returns DiscordBotSettings(
            id = 1,
            guildId = "guild_1",
            volume = 100,
            followedUser = "different_user",
            lastChannel = null
        )

        discordEventHandler.onGuildVoiceUpdate(event)

        verify(exactly = 0) { audioManager.openAudioConnection(any()) }
        verify(exactly = 0) { audioManager.closeAudioConnection() }
    }

    @Test
    internal fun testOnGuildVoiceUpdate_FollowedUserJoinedChannel_OpensAudioConnection() {
        val event = mockk<GuildVoiceUpdateEvent>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)
        val channelJoined = mockk<AudioChannelUnion>(relaxed = true)

        every { event.member.user.isBot } returns false
        every { event.member.id } returns "user_1"
        every { event.guild.id } returns "guild_1"
        every { event.guild.audioManager } returns audioManager
        every { event.channelJoined } returns channelJoined
        every { discordBotSettingsRepository.findOneByGuildId("guild_1") } returns DiscordBotSettings(
            id = 1,
            guildId = "guild_1",
            volume = 100,
            followedUser = "user_1",
            lastChannel = null
        )

        discordEventHandler.onGuildVoiceUpdate(event)

        verify { audioManager.openAudioConnection(channelJoined) }
        verify(exactly = 0) { audioManager.closeAudioConnection() }
    }

    @Test
    internal fun testOnGuildVoiceUpdate_FollowedUserLeftVoice_ClosesAudioConnection() {
        val event = mockk<GuildVoiceUpdateEvent>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)

        every { event.member.user.isBot } returns false
        every { event.member.id } returns "user_1"
        every { event.guild.id } returns "guild_1"
        every { event.guild.audioManager } returns audioManager
        every { event.channelJoined } returns null
        every { discordBotSettingsRepository.findOneByGuildId("guild_1") } returns DiscordBotSettings(
            id = 1,
            guildId = "guild_1",
            volume = 100,
            followedUser = "user_1",
            lastChannel = null
        )

        discordEventHandler.onGuildVoiceUpdate(event)

        verify { audioManager.closeAudioConnection() }
        verify(exactly = 0) { audioManager.openAudioConnection(any()) }
    }

    @Test
    internal fun testOnGuildLeave_CleansUpGuildAndNotifiesTelegram() {
        val event = mockk<GuildLeaveEvent>(relaxed = true)
        every { event.guild.id } returns "guild_1"
        every { event.guild.name } returns "My Guild"

        discordEventHandler.onGuildLeave(event)

        verify { telegramNotifier.sendPrivateMessage(match { it.contains("Left a guild") && it.contains("My Guild") }) }
        verify { discordBotService.cleanUpGuild("guild_1") }
    }

    @Test
    internal fun testOnSlashCommandInteraction_NotInGuild_RepliesEphemeral() {
        val event = mockk<SlashCommandInteractionEvent>(relaxed = true)
        every { event.guild } returns null

        discordEventHandler.onSlashCommandInteraction(event)

        verify { event.reply("Please use that command in a server's text channel.") }
    }

    @Test
    internal fun testOnSlashCommandInteraction_ValidCommand_ExecutesAndLogs() {
        val event = mockk<SlashCommandInteractionEvent>(relaxed = true)
        val guild = mockk<Guild>(relaxed = true)
        val user = mockk<User>(relaxed = true)
        every { event.guild } returns guild
        every { event.name } returns "test_cmd"
        every { event.user } returns user
        every { user.id } returns "user_123"

        discordEventHandler.onSlashCommandInteraction(event)

        verify { command.handleCommand(event) }
        verify { analyticsService.logCommandUsage("user_123", "test_cmd") }
    }

    @Test
    internal fun testOnSlashCommandInteraction_CommandThrows_SendsNotificationAndRepliesError() {
        val event = mockk<SlashCommandInteractionEvent>(relaxed = true)
        val guild = mockk<Guild>(relaxed = true)
        val user = mockk<User>(relaxed = true)
        every { event.guild } returns guild
        every { event.name } returns "test_cmd"
        every { event.user } returns user
        every { user.id } returns "user_123"
        every { event.isAcknowledged } returns false
        every { command.handleCommand(event) } throws RuntimeException("Boom!")

        discordEventHandler.onSlashCommandInteraction(event)

        verify { telegramNotifier.sendPrivateMessage(match { it.contains("Slash command failed") && it.contains("Boom!") }) }
        verify { event.reply("Something went wrong while executing this command.") }
    }

    @Test
    internal fun testOnReady_WithLastVoiceChannel_ReconnectsAndClearsSetting() {
        val event = mockk<ReadyEvent>(relaxed = true)
        val jda = mockk<JDA>(relaxed = true)
        val guild = mockk<Guild>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)
        val voiceChannel = mockk<net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel>(relaxed = true)
        val settings = DiscordBotSettings(
            id = 1,
            guildId = "guild_1",
            volume = 100,
            followedUser = null,
            lastChannel = "channel_1"
        )

        every { event.jda } returns jda
        every { jda.getGuildById("guild_1") } returns guild
        every { guild.audioManager } returns audioManager
        every { guild.getVoiceChannelById("channel_1") } returns voiceChannel
        every { discordBotSettingsRepository.findAll() } returns listOf(settings)
        every { discordBotSettingsRepository.save(any()) } answers { firstArg() }

        discordEventHandler.onReady(event)

        verify { audioManager.openAudioConnection(voiceChannel) }
        verify { discordBotSettingsRepository.save(match { it.lastChannel == null }) }
    }

    @Test
    internal fun testOnReady_WithLastStageChannel_ReconnectsAndClearsSetting() {
        val event = mockk<ReadyEvent>(relaxed = true)
        val jda = mockk<JDA>(relaxed = true)
        val guild = mockk<Guild>(relaxed = true)
        val audioManager = mockk<AudioManager>(relaxed = true)
        val stageChannel = mockk<net.dv8tion.jda.api.entities.channel.concrete.StageChannel>(relaxed = true)
        val settings = DiscordBotSettings(
            id = 1,
            guildId = "guild_1",
            volume = 100,
            followedUser = null,
            lastChannel = "channel_stage"
        )

        every { event.jda } returns jda
        every { jda.getGuildById("guild_1") } returns guild
        every { guild.audioManager } returns audioManager
        every { guild.getVoiceChannelById("channel_stage") } returns null
        every { guild.getStageChannelById("channel_stage") } returns stageChannel
        every { discordBotSettingsRepository.findAll() } returns listOf(settings)
        every { discordBotSettingsRepository.save(any()) } answers { firstArg() }

        discordEventHandler.onReady(event)

        verify { audioManager.openAudioConnection(stageChannel) }
        verify { discordBotSettingsRepository.save(match { it.lastChannel == null }) }
    }
}

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

package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.component.DISCORD_TOKEN
import com.github.mrbean355.roons.discord.commands.BotCommand
import com.github.mrbean355.roons.discord.commands.queueEphemeralReply
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.loadSettings
import com.github.mrbean355.roons.repository.takeStartupMessage
import com.github.mrbean355.roons.repository.takeUpdateSlashCommandsFlag
import com.github.mrbean355.roons.telegram.TelegramNotifier
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission.MESSAGE_READ
import net.dv8tion.jda.api.Permission.MESSAGE_WRITE
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class DiscordBot @Autowired constructor(
    private val commands: List<BotCommand>,
    private val discordBotUserRepository: DiscordBotUserRepository,
    private val discordBotSettingsRepository: DiscordBotSettingsRepository,
    private val metadataRepository: MetadataRepository,
    private val soundStore: SoundStore,
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger,
    @Qualifier(DISCORD_TOKEN) private val token: String
) : ListenerAdapter() {

    private val botScope = CoroutineScope(IO + SupervisorJob())
    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager> = mutableMapOf()
    private val bot: JDA = JDABuilder.createDefault(token)
        .setActivity(Activity.playing("Get the roons!"))
        .addEventListeners(this)
        .build()

    @Value("\${roons.slashCommands.testGuild:false}")
    private var testSlashCommands: Boolean = false

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    override fun onReady(event: ReadyEvent) = runBlocking(IO) {
        // Register slash commands:
        if (testSlashCommands) {
            logger.info("Testing slash commands")
            event.jda.guilds.forEach { guild ->
                guild.updateCommands()
                    .addCommands(commands.map { CommandData(it.name, it.description).apply(it::build) })
                    .queue()
            }
        } else if (metadataRepository.takeUpdateSlashCommandsFlag()) {
            logger.info("Updating slash commands")
            bot.updateCommands()
                .addCommands(commands.map { CommandData(it.name, it.description).apply(it::build) })
                .queue()
        }

        // Show startup message if there is one:
        val message = metadataRepository.takeStartupMessage()
            ?.replace("\\n", "\n")

        if (message != null && message.isNotBlank()) {
            supervisorScope {
                bot.guilds.forEach {
                    launch {
                        it.findWelcomeChannel()?.typeMessage(message)
                    }
                }
            }
        }

        val reconnects = AtomicInteger()
        // Reconnect to previous voice channels:
        supervisorScope {
            discordBotSettingsRepository.findAll().forEach { settings ->
                launch {
                    settings.lastChannel?.let { lastChannel ->
                        val guild = bot.getGuildById(settings.guildId)
                        val channel = guild?.getVoiceChannelById(lastChannel)
                        if (channel != null) {
                            guild.audioManager.openAudioConnection(channel)
                            reconnects.incrementAndGet()
                        }
                        discordBotSettingsRepository.save(settings.copy(lastChannel = null))
                    }
                }
            }
        }

        telegramNotifier.sendPrivateMessage("⚙️ <b>Started up</b>:\nReconnected to <b>${reconnects.get()}</b> voice channels.")
    }

    /** Try to play the given [soundFileName] in a guild. Determines the guild from the [token]. */
    fun playSound(discordBotUser: DiscordBotUser, soundFileName: String, volume: Int, rate: Int): Boolean {
        val guild = bot.getGuildById(discordBotUser.guildId) ?: return false
        val file = soundStore.getFile(soundFileName) ?: return false
        val masterVolume = discordBotSettingsRepository.loadSettings(discordBotUser.guildId).volume
        val finalVolume = (volume * masterVolume) / 100
        return playSound(guild, file.absolutePath, finalVolume, rate)
    }

    /** Disconnect from voice channels when shutting down. */
    fun shutdown() = runBlocking(IO) {
        bot.presence.setStatus(OnlineStatus.OFFLINE)
        val connectedGuilds = bot.guilds.filter { it.isConnected() }
        supervisorScope {
            connectedGuilds.forEach { guild ->
                launch {
                    val settings = discordBotSettingsRepository.loadSettings(guild.id)
                    val currentVoiceChannel = guild.selfMember.voiceState?.channel?.id
                    discordBotSettingsRepository.save(settings.copy(lastChannel = currentVoiceChannel))
                    guild.audioManager.closeAudioConnection()
                }
            }
        }
        telegramNotifier.sendPrivateMessage("⚙️ <b>Shutting down</b>:\nDisconnected from <b>${connectedGuilds.size}</b> voice channels.")
    }

    fun getGuilds(): List<Guild> = bot.guilds

    fun getGuildById(id: String): Guild? {
        return bot.getGuildById(id)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        botScope.launch {
            if (event.author.isBot || !event.isFromType(ChannelType.TEXT)) {
                return@launch
            }
            val message = event.message.contentRaw.trim()
            if (message.startsWith("!volume")) {
                event.handleDeprecatedCommand("/volume")
                return@launch
            }
            when (message) {
                "!help" -> event.handleDeprecatedCommand("/help")
                "!roons" -> event.handleDeprecatedCommand("/join")
                "!seeya" -> event.handleDeprecatedCommand("/leave")
                "!magic" -> event.handleDeprecatedCommand("/magicnumber")
                "!follow" -> event.handleDeprecatedCommand("/follow")
                "!unfollow" -> event.handleDeprecatedCommand("/unfollow")
            }
        }
    }

    private fun MessageReceivedEvent.handleDeprecatedCommand(command: String) {
        textChannel.typeMessage(
            "⚠️ My commands have been upgraded to slash commands. Please use `$command` instead.\n" +
                    "*Note: it may take up to an hour for the commands to appear in your server.*"
        )
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        botScope.launch {
            val guild = event.guild
            telegramNotifier.sendPrivateMessage("🎉 <b>Joined a guild</b>:\n${guild.name}, ${guild.region}, ${guild.memberCount} members")

            guild.findWelcomeChannel()?.typeMessage(
                """
                **ALLO, ${guild.name}!** :wave:
                
                Type `/join` for me to join your current voice channel.
                Type `/leave` when you want me to leave the voice channel.
                Type `/follow` for me to follow you when you join & leave voice channels.
                Type `/help` for more commands.
                """.trimIndent()
            )
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        botScope.launch {
            telegramNotifier.sendPrivateMessage("😔 <b>Left a guild</b>:\n${event.guild.name}")
            val guildId = event.guild.id
            discordBotUserRepository.deleteByGuildId(guildId)
            discordBotSettingsRepository.deleteByGuildId(guildId)
        }
    }

    override fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
        botScope.launch {
            if (event.member.user.isBot) {
                return@launch
            }
            val settings = discordBotSettingsRepository.findOneByGuildId(event.guild.id) ?: return@launch
            if (settings.followedUser == event.member.id) {
                when (event) {
                    is GuildVoiceJoinEvent -> event.guild.audioManager.openAudioConnection(event.channelJoined)
                    is GuildVoiceMoveEvent -> event.guild.audioManager.openAudioConnection(event.channelJoined)
                    is GuildVoiceLeaveEvent -> event.guild.audioManager.closeAudioConnection()
                }
            }
        }
    }

    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        botScope.launch {
            if (!event.author.isBot) {
                event.message.channel.typeMessage(":no_entry: Please send me commands through a text channel in your server.")
            }
        }
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.guild == null) {
            event.queueEphemeralReply("Please use that command in a server's text channel.")
            return
        }
        commands.find { it.name == event.name }
            ?.process(event)
    }

    /** @return a guild-specific [GuildMusicManager]. */
    private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
        return synchronized(this) {
            val manager = musicManagers.getOrPut(guild.idLong) { GuildMusicManager(guild.id, playerManager, discordBotSettingsRepository) }
            guild.audioManager.sendingHandler = manager.getSendHandler()
            manager
        }
    }

    private fun playSound(guild: Guild, filePath: String, volume: Int, rate: Int): Boolean {
        if (!guild.isConnected()) {
            logger.warn("Tried to play sound while not in voice channel.")
            return false
        }
        val manager = getGuildAudioPlayer(guild)
        playerManager.loadItemOrdered(manager, filePath, object : AudioLoadResultHandler {

            override fun trackLoaded(track: AudioTrack) {
                manager.scheduler.queue(track, volume, rate)
            }

            override fun playlistLoaded(playlist: AudioPlaylist?) {
                logger.warn("Somehow loaded a playlist; ignoring.")
            }

            override fun noMatches() {
                logger.error("No matches found for '$filePath'.")
            }

            override fun loadFailed(exception: FriendlyException?) {
                logger.error("Failed to load track '$filePath': $exception")
            }
        })
        return true
    }

    /** Sends a `message to `this` channel, after pretending to type. */
    private fun MessageChannel.typeMessage(message: String) {
        sendTyping().queue {
            sendMessage(message).queue()
        }
    }

    /** @return `true` if the bot is connected to a voice channel in `this` guild. */
    private fun Guild.isConnected(): Boolean {
        return audioManager.isConnected
    }

    /** @return the first (if any) [TextChannel] which the bot can read & write to. */
    private fun Guild.findWelcomeChannel(): TextChannel? {
        val self = selfMember
        val defaultChannel = defaultChannel
        if (defaultChannel != null) {
            if (self.canReadAndWrite(defaultChannel)) {
                return defaultChannel
            }
        }
        return textChannels.firstOrNull {
            self.canReadAndWrite(it)
        }
    }

    /** @return `true` if this [Member] can read & write to the [channel]. */
    private fun Member.canReadAndWrite(channel: TextChannel): Boolean {
        return hasPermission(channel, MESSAGE_READ, MESSAGE_WRITE)
    }
}
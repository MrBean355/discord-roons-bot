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
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.loadSettings
import com.github.mrbean355.roons.repository.takeStartupMessage
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
import net.dv8tion.jda.api.Permission.VOICE_CONNECT
import net.dv8tion.jda.api.Permission.VOICE_SPEAK
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private const val HELP_URL = "https://github.com/MrBean355/admiralbulldog-sounds/wiki/Discord-Bot"

@Component
class DiscordBot @Autowired constructor(
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

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    override fun onReady(event: ReadyEvent) = runBlocking(IO) {
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

    /** Dump the current status for each joined guild. */
    fun dumpStatus(): String {
        val builder = StringBuilder()
        val (activeGuilds, inactiveGuilds) = bot.guilds
            .sortedBy { it.name.toLowerCase() }
            .partition { it.isConnected() }

        builder.append("<h1>In ${activeGuilds.size + inactiveGuilds.size} Total Guilds</h1>")
        builder.append("<h2>Active Guilds</h2>")
        builder.append("<ul>")
        activeGuilds.forEach {
            builder.append("<li>")
                .append(it.name).append(" | ")
                .append(it.memberCount).append(" members | ")
                .append(it.region.getName()).append(" | ")
                .append("in voice channel: ${it.audioManager.connectedChannel?.name}")
                .append("</li>")
        }
        builder.append("</ul>")

        builder.append("<h2>Inactive Guilds</h2>")
        builder.append("<ul>")
        inactiveGuilds.forEach {
            builder.append("<li>")
                .append(it.name).append(" | ")
                .append(it.memberCount).append(" members | ")
                .append(it.region.getName())
                .append("</li>")
        }
        builder.append("</ul>")
        return builder.toString()
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
                volume(message, event)
                return@launch
            }
            when (message) {
                "!help" -> help(event)
                "!roons" -> join(event)
                "!seeya" -> leave(event)
                "!magic" -> magic(event)
                "!follow" -> follow(event)
                "!unfollow" -> unfollow(event)
            }
        }
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        botScope.launch {
            val guild = event.guild
            telegramNotifier.sendPrivateMessage("🎉 <b>Joined a guild</b>:\n${guild.name}, ${guild.region}, ${guild.memberCount} members")

            guild.findWelcomeChannel()?.typeMessage(
                """
                **ALLO, ${guild.name}!** :wave:
                
                Type `!roons` for me to join your current voice channel.
                Type `!seeya` when you want me to leave the voice channel.
                Type `!follow` for me to follow you when you join & leave voice channels.
                Type `!help` for more commands.
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

    private fun volume(message: String, event: MessageReceivedEvent) {
        val parts = message.split(' ').filter { it.isNotBlank() }
        // Get the volume:
        if (parts.size == 1) {
            val volume = getGuildAudioPlayer(event.guild).getMasterVolume()
            event.channel.typeMessage("My volume is at `${volume}%` :loud_sound:")
            return
        }
        // Set the volume:
        if (parts.size == 2) {
            parts[1].toIntOrNull()?.coerceVolume()?.let { volume ->
                getGuildAudioPlayer(event.guild).setMasterVolume(volume)
                event.channel.typeMessage("My volume has been set to `${volume}%` :ok_hand:")
                return
            }
        }
        // Invalid command:
        event.channel.typeMessage(
            """
            I'm not sure what you meant :disappointed:
            Type `!volume` to check the volume level.
            Type `!volume 50` to set the volume to 50%.
            """.trimIndent()
        )
    }

    private fun help(event: MessageReceivedEvent) {
        event.channel.typeMessage(
            """
            **My available commands**
            
            - `!help` :arrow_right: send this message
            - `!roons` :arrow_right: join your current voice channel
            - `!seeya` :arrow_right: leave the current voice channel
            - `!magic` :arrow_right: send a private message with your magic number
            - `!follow` :arrow_right: follow you when you join & leave voice channels
            - `!unfollow` :arrow_right: stop following you
            - `!volume` :arrow_right: show the current volume
            - `!volume x` :arrow_right: set the current volume to x% (example: `!volume 50`)
            
            For more info or to log a bug, please visit: $HELP_URL
            """.trimIndent()
        )
    }

    private fun join(event: MessageReceivedEvent) {
        val channel = event.member?.voiceState?.channel
        if (channel == null) {
            event.textChannel.typeMessage("Please join a voice channel first, then type the command again.")
            return
        }
        if (event.guild.isConnected()) {
            val currentChannel = event.guild.audioManager.connectedChannel ?: return
            if (currentChannel.idLong == channel.idLong) {
                event.textChannel.typeMessage("I'm already connected to `${currentChannel.name}`.")
                return
            }
        }
        val self = event.guild.selfMember
        if (!self.hasPermission(channel, VOICE_CONNECT)) {
            event.textChannel.typeMessage("I don't have permission to connect to `${channel.name}`.")
        } else if (!self.hasPermission(channel, VOICE_SPEAK)) {
            event.textChannel.typeMessage("I don't have permission to speak in `${channel.name}`.")
        } else {
            runCatching { event.guild.audioManager.openAudioConnection(channel) }
                .onSuccess { event.textChannel.typeMessage("I've connected to `${channel.name}`.") }
                .onFailure { event.textChannel.typeMessage("I can't connect to `${channel.name}` at the moment.") }
        }
    }

    private fun leave(event: MessageReceivedEvent) {
        val audioManager = event.guild.audioManager
        if (event.guild.isConnected()) {
            val channelName = audioManager.connectedChannel?.name
            event.guild.audioManager.closeAudioConnection()
            event.textChannel.typeMessage("I've disconnected from `$channelName`.")
        } else {
            event.textChannel.typeMessage("I'm not connected to a voice channel.")
        }
    }

    private fun magic(event: MessageReceivedEvent) {
        val discordBotUser = findOrCreateUser(event.author, event.guild)
        event.author.openPrivateChannel().queue {
            it.typeMessage(
                """
                Here's your magic number for **${event.guild.name}**:
                :point_right: `${discordBotUser.token}` :point_left:
                *Don't share this with anyone!*
                """.trimIndent()
            )
        }
        event.channel.typeMessage("Sent you a private message, ${event.author.asMention} :thumbsup:")
    }

    private fun follow(event: MessageReceivedEvent) {
        val settings = discordBotSettingsRepository.loadSettings(event.guild.id)
        val followedUser = settings.followedUser
        if (followedUser == event.author.id) {
            event.textChannel.typeMessage("I'm already following ${event.author.asMention} :shrug:")
            return
        }
        discordBotSettingsRepository.save(settings.copy(followedUser = event.author.id))
        event.member?.voiceState?.channel?.let {
            event.guild.audioManager.openAudioConnection(it)
        }
        val insteadOf = if (followedUser != null) {
            val previousUser = bot.getUserById(followedUser)
            "instead of ${previousUser?.asMention ?: "unknown"} "
        } else ""
        event.textChannel.typeMessage(
            """
            I'm now following ${event.author.asMention} ${insteadOf}:ok_hand:
            Type `!unfollow` and I'll stop.
            """.trimIndent()
        )
    }

    private fun unfollow(event: MessageReceivedEvent) {
        val settings = discordBotSettingsRepository.loadSettings(event.guild.id)
        val followedUser = settings.followedUser
        if (followedUser == null) {
            event.textChannel.typeMessage("I'm not following anyone :shrug:")
            return
        }
        discordBotSettingsRepository.save(settings.copy(followedUser = null))
        val user = bot.getUserById(followedUser) ?: return
        event.textChannel.typeMessage("I've stopped following ${user.asMention} :ok_hand:")
    }

    private fun findOrCreateUser(user: User, guild: Guild): DiscordBotUser {
        val userId = user.id
        val guildId = guild.id
        return discordBotUserRepository.findOneByDiscordUserIdAndGuildId(userId, guildId)
            ?: discordBotUserRepository.save(DiscordBotUser(0, userId, guildId, UUID.randomUUID().toString()))
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
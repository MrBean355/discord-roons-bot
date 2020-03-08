package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.component.TOKEN
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
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission.*
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
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID
import javax.annotation.PreDestroy

private const val HELP_URL = "https://github.com/MrBean355/admiralbulldog-sounds/wiki/Discord-Bot"

@Component
class DiscordBot @Autowired constructor(
        private val discordBotUserRepository: DiscordBotUserRepository,
        private val discordBotSettingsRepository: DiscordBotSettingsRepository,
        private val metadataRepository: MetadataRepository,
        private val soundStore: SoundStore,
        private val telegramNotifier: TelegramNotifier,
        private val logger: Logger,
        @Qualifier(TOKEN) private val token: String
) : ListenerAdapter() {

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager> = mutableMapOf()
    private val bot: JDA = JDABuilder(token)
            .setActivity(Activity.playing("Get the roons!"))
            .addEventListeners(this)
            .build()

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    override fun onReady(event: ReadyEvent) {
        // Show startup message if there is one:
        val message = metadataRepository.takeStartupMessage()
                ?.replace("\\n", "\n")

        if (message != null && message.isNotBlank()) {
            logger.info("Sending startup message to ${bot.guilds.size} guilds:\n$message")
            bot.guilds.forEach {
                it.findWelcomeChannel()?.typeMessage(message)
            }
        }

        var reconnects = 0
        // Reconnect to previous voice channels:
        discordBotSettingsRepository.findAll().forEach { settings ->
            settings.lastChannel?.let { lastChannel ->
                val guild = bot.getGuildById(settings.guildId)
                val channel = guild?.getVoiceChannelById(lastChannel)
                if (channel != null) {
                    guild.audioManager.openAudioConnection(channel)
                    ++reconnects
                }
                discordBotSettingsRepository.save(settings.copy(lastChannel = null))
            }
        }

        telegramNotifier.sendMessage("Started up!\nReconnected to $reconnects voice channels.")
    }

    /** Try to play the given [soundFileName] in a guild. Determines the guild from the [token]. */
    fun playSound(token: String, soundFileName: String): Boolean {
        val discordBotUser = discordBotUserRepository.findOneByToken(token)
        if (discordBotUser == null) {
            logger.error("Unknown token: $token")
            return false
        }
        val guild = bot.getGuildById(discordBotUser.guildId) ?: return false
        val file = soundStore.getFile(soundFileName) ?: return false
        return playSound(guild, file.absolutePath)
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
    @PreDestroy
    fun onPreDestroy() {
        bot.presence.setStatus(OnlineStatus.OFFLINE)
        val connectedGuilds = bot.guilds.filter { it.isConnected() }
        connectedGuilds.forEach { guild ->
            logger.info("Disconnecting from guild: ${guild.name}.")
            val settings = discordBotSettingsRepository.loadSettings(guild.id)
            val currentVoiceChannel = guild.selfMember.voiceState?.channel?.id
            discordBotSettingsRepository.save(settings.copy(lastChannel = currentVoiceChannel))
            guild.audioManager.closeAudioConnection()
        }
        telegramNotifier.sendMessage("Shutting down...\nDisconnected from ${connectedGuilds.size} voice channels.")
    }

    fun getGuildById(id: String): Guild? {
        return bot.getGuildById(id)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || !event.isFromType(ChannelType.TEXT)) {
            return
        }
        val message = event.message.contentRaw.trim()
        if (message.startsWith("!volume")) {
            volume(message, event)
            return
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

    override fun onGuildJoin(event: GuildJoinEvent) {
        val guild = event.guild
        telegramNotifier.sendMessage("""
            Joined a new guild:
            ${guild.name}
            ${guild.region}
            ${guild.memberCount} members
        """.trimIndent())

        val channel = guild.findWelcomeChannel() ?: return
        channel.typeMessage("""
            **Hello, ${guild.name}!** :wave:
            
            Type `!roons` for me to join your current voice channel.
            Type `!seeya` when you want me to leave the voice channel.
            Type `!follow` for me to follow you when you join & leave voice channels.
            Type `!help` for more commands.
        """.trimIndent())
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        telegramNotifier.sendMessage("Left a guild: ${event.guild.name}")
    }

    override fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
        if (event.member.user.isBot) {
            return
        }
        val settings = discordBotSettingsRepository.findOneByGuildId(event.guild.id) ?: return
        if (settings.followedUser == event.member.id) {
            when (event) {
                is GuildVoiceJoinEvent -> event.guild.audioManager.openAudioConnection(event.channelJoined)
                is GuildVoiceMoveEvent -> event.guild.audioManager.openAudioConnection(event.channelJoined)
                is GuildVoiceLeaveEvent -> event.guild.audioManager.closeAudioConnection()
            }
        }
    }

    private fun volume(message: String, event: MessageReceivedEvent) {
        val parts = message.split(' ').filter { it.isNotBlank() }
        // Get the volume:
        if (parts.size == 1) {
            val volume = getGuildAudioPlayer(event.guild).getVolume()
            event.channel.typeMessage("My volume is at ${volume}% :loud_sound:")
            return
        }
        // Set the volume:
        if (parts.size == 2) {
            parts[1].toIntOrNull()?.coerceVolume()?.let { volume ->
                getGuildAudioPlayer(event.guild).setVolume(volume)
                event.channel.typeMessage("My volume has been set to ${volume}% :ok_hand:")
                return
            }
        }
        // Invalid command:
        event.channel.typeMessage("I'm not sure what you meant :disappointed:\n" +
                "Type `!volume` to check the volume level.\n" +
                "Type `!volume 50` to set the volume to 50%.")
    }

    private fun help(event: MessageReceivedEvent) {
        event.channel.typeMessage("**My available commands**\n" +
                "\n" +
                "- `!help` :arrow_right: send this message\n" +
                "- `!roons` :arrow_right: join your current voice channel\n" +
                "- `!seeya` :arrow_right: leave the current voice channel\n" +
                "- `!magic` :arrow_right: send a private message with your magic number\n" +
                "- `!follow` :arrow_right: follow you when you join & leave voice channels\n" +
                "- `!unfollow` :arrow_right: stop following you\n" +
                "- `!volume` :arrow_right: show the current volume\n" +
                "- `!volume x` :arrow_right: set the current volume to x% (example: `!volume 50`)\n" +
                "\n" +
                "For more info or to log a bug, please visit: $HELP_URL")
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
            it.typeMessage("Here's your magic number: `${discordBotUser.token}`\n" +
                    "Please don't give this to anyone else!\n" +
                    "In the app, click \"Discord bot\" and tick the box to enable me.\n" +
                    "Then paste your magic number in the text box.")
        }
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
        event.textChannel.typeMessage("I'm now following ${event.author.asMention} ${insteadOf}:ok_hand:\n" +
                "Type `!unfollow` and I'll stop")
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

    private fun playSound(guild: Guild, filePath: String): Boolean {
        if (!guild.isConnected()) {
            logger.warn("Tried to play sound while not in voice channel.")
            return false
        }
        val manager = getGuildAudioPlayer(guild)
        playerManager.loadItemOrdered(manager, filePath, object : AudioLoadResultHandler {

            override fun trackLoaded(track: AudioTrack) {
                manager.scheduler.queue(track)
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
        return audioManager.isConnected || audioManager.isAttemptingToConnect
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
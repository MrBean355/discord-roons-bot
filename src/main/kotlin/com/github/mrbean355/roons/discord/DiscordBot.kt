package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.component.TOKEN
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
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
import net.dv8tion.jda.api.Permission.VOICE_CONNECT
import net.dv8tion.jda.api.Permission.VOICE_SPEAK
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
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
        private val soundStore: SoundStore,
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

    /** Try to play the given [soundFileName] in a guild. Determines the guild from the [token]. */
    fun playSound(token: String, soundFileName: String): Boolean {
        val discordBotUser = discordBotUserRepository.findOneByToken(token)
        if (discordBotUser == null) {
            logger.error("Unknown token: $token")
            return false
        }
        val guild = bot.getGuildById(discordBotUser.guildId) ?: return false
        return playSound(guild, soundStore.getFilePath(soundFileName))
    }

    /** Dump the current status for each joined guild. */
    fun dumpStatus(): String {
        val builder = StringBuilder()
        val guilds = bot.guilds
        builder.append("Currently in ${guilds.size} guilds:<ul>")
        guilds.forEach {
            builder.append("<li>")
                    .append(it.name).append(" | ")
                    .append(it.members.size).append(" members | ")
                    .append(it.region.getName()).append(" | ")
            if (it.isConnected()) {
                builder.append("in voice channel: ${it.audioManager.connectedChannel?.name}")
            } else {
                builder.append("idle")
            }
            builder.append("</li>")
        }
        builder.append("</ul>")
        return builder.toString()
    }

    /** Disconnect from voice channels when shutting down. */
    @PreDestroy
    fun onPreDestroy() {
        bot.presence.setStatus(OnlineStatus.OFFLINE)
        bot.guilds.filter { it.isConnected() }.forEach {
            logger.info("Disconnecting from guild: ${it.name}.")
            it.audioManager.closeAudioConnection()
        }
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
        event.channel.typeMessage("My available commands:\n" +
                "- `!help` :arrow_right: this message\n" +
                "- `!roons` :arrow_right: join your current voice channel\n" +
                "- `!seeya` :arrow_right: leave the current voice channel\n" +
                "- `!magic` :arrow_right: send a private message with your magic number\n" +
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
                    "Paste this into the app on this screen:\n" +
                    "https://i.imgur.com/Ugh9yzG.png")
        }
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
            val manager = musicManagers.getOrPut(guild.idLong) { GuildMusicManager(playerManager) }
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
}
package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

interface DiscordBot {
    fun start()
    fun playerManager(guild: Guild): GuildPlayerManager

    fun playSound(guild: Guild): Boolean
    fun playSoundRemote(token: String)
    fun getVolume(guild: Guild): Int
    fun setVolume(guild: Guild, newVolume: Int)
    fun enableTest(token: String)
}

class RunesDiscordBot(apiToken: String) : DiscordBot {
    private val client = DiscordClientBuilder(apiToken)
            .setInitialPresence(Presence.online(Activity.playing("Get the roons!")))
            .build()
    private val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val guildPlayerManagers: MutableMap<Long, GuildPlayerManager> = ConcurrentHashMap()
    private val commands = setOf(
            HelpCommand(this),
            JoinCommand(this),
            LeaveCommand(this),
            MagicCommand(this),
            TestCommand(this),
            VolumeCommand(this))

    init {
        audioPlayerManager.configuration.frameBufferFactory = AudioFrameBufferFactory { bufferDuration, format, stopping ->
            NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
        }
        AudioSourceManagers.registerLocalSource(audioPlayerManager)
    }

    override fun start() {
        client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe(this::processMessage)
        client.login().subscribe()
    }

    override fun playerManager(guild: Guild): GuildPlayerManager {
        return getGuildPlayerManager(guild)
    }

    override fun playSound(guild: Guild): Boolean {
        val guildPlayerManager = getGuildPlayerManager(guild)
        if (guildPlayerManager.voiceConnection == null) {
            return false
        }
        audioPlayerManager.loadItemOrdered(guild.id.asLong(), "roons.mp3", DelegateResultHandler(guildPlayerManager.audioPlayer))
        return true
    }

    override fun playSoundRemote(token: String) {
        if (UserStore.userExists(token)) {
            UserStore.findUserId(token)?.let { ids ->
                client.getGuildById(ids.second)
                        .doOnNext { playSound(it) }
                        .subscribe()
            }
        }
    }

    override fun getVolume(guild: Guild): Int {
        return getGuildPlayerManager(guild).audioPlayer.volume
    }

    override fun setVolume(guild: Guild, newVolume: Int) {
        getGuildPlayerManager(guild).audioPlayer.volume = newVolume
    }

    override fun enableTest(token: String) {
        // TODO: Enable test mode.
    }

    private fun processMessage(event: MessageCreateEvent) {
        Mono.justOrEmpty(event.message.content)
                .flatMap { content ->
                    Flux.fromIterable(commands)
                            .filter { command -> content.startsWith('!' + command.input) }
                            .flatMap { command -> command.execute(event) }
                            .next()
                }
                .subscribe()
    }

    private fun getGuildPlayerManager(guild: Guild): GuildPlayerManager {
        var manager = guildPlayerManagers[guild.id.asLong()]
        if (manager == null) {
            manager = GuildPlayerManager(audioPlayerManager)
            guildPlayerManagers[guild.id.asLong()] = manager
        }
        return manager
    }
}

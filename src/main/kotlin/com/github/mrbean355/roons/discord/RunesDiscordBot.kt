package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.VoiceChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

interface DiscordBot {
    fun joinVoiceChannel(guild: Guild, voiceChannel: VoiceChannel): Mono<Void>
    fun leaveVoiceChannel(guild: Guild): Boolean
    fun playSound(guild: Guild): Boolean
    fun setVolume(guild: Guild, newVolume: Int)
}

class RunesDiscordBot(token: String) : DiscordBot {
    private val client = DiscordClientBuilder(token).build()
    private val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val guildPlayerManagers: MutableMap<Long, GuildPlayerManager> = ConcurrentHashMap()
    private val commands = mapOf(
            "halp" to HelpCommand(this),
            "roons" to JoinCommand(this),
            "test" to TestCommand(this),
            "volume" to VolumeCommand(this),
            "rekt" to LeaveCommand(this))

    init {
        audioPlayerManager.configuration.frameBufferFactory = AudioFrameBufferFactory { bufferDuration, format, stopping ->
            NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
        }
        AudioSourceManagers.registerLocalSource(audioPlayerManager)
    }

    fun start() {
        client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe(this::processMessage)
        client.login().block()
    }

    private fun processMessage(event: MessageCreateEvent) {
        Mono.justOrEmpty(event.message.content)
                .flatMap { content ->
                    Flux.fromIterable(commands.entries)
                            .filter { entry -> content.startsWith('!' + entry.key) }
                            .flatMap { entry -> entry.value.execute(event) }
                            .next()
                }
                .subscribe()
    }

    override fun joinVoiceChannel(guild: Guild, voiceChannel: VoiceChannel): Mono<Void> {
        val guildPlayerManager = getGuildPlayerManager(guild)
        return voiceChannel.join { it.setProvider(guildPlayerManager.audioProvider) }
                .doOnNext { guildPlayerManager.voiceConnection = it }
                .then()
    }

    override fun leaveVoiceChannel(guild: Guild): Boolean {
        val guildPlayerManager = getGuildPlayerManager(guild)
        val connection = guildPlayerManager.voiceConnection
        return if (connection != null) {
            connection.disconnect()
            guildPlayerManager.voiceConnection = null
            true
        } else {
            false
        }
    }

    override fun playSound(guild: Guild): Boolean {
        val guildPlayerManager = getGuildPlayerManager(guild)
        if (guildPlayerManager.voiceConnection == null) {
            return false
        }
        audioPlayerManager.loadItemOrdered(guild.id.asLong(), "roons.mp3", DelegateResultHandler(guildPlayerManager.audioPlayer))
        return true
    }

    override fun setVolume(guild: Guild, newVolume: Int) {
        getGuildPlayerManager(guild).audioPlayer.volume = newVolume
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

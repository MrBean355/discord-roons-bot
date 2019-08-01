package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.COMMAND_PREFIX
import com.github.mrbean355.roons.SOUND_FILE_NAME
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
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

interface SoundEffectPlayer {
    fun playSound(token: String)
}

class RunesDiscordBot(apiToken: String) : CommandCallbacks, SoundEffectPlayer {
    private val client = DiscordClientBuilder(apiToken)
            .setInitialPresence(Presence.online(Activity.playing("Get the roons!")))
            .build()
    private val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val guildPlayerManagers: MutableMap<Long, GuildPlayerManager> = ConcurrentHashMap()
    private val commands = allCommands(this)
    var testModeDelegate: (String) -> Unit = {}

    init {
        audioPlayerManager.configuration.frameBufferFactory = AudioFrameBufferFactory { bufferDuration, format, stopping ->
            NonAllocatingAudioFrameBuffer(bufferDuration, format, stopping)
        }
        AudioSourceManagers.registerLocalSource(audioPlayerManager)
    }

    fun startAsync() {
        client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe(this::processMessage)
        client.login().subscribe()
    }

    override fun getPlayerManager(guild: Guild): GuildPlayerManager {
        return getGuildPlayerManager(guild)
    }

    override fun enableTestMode(token: String) {
        testModeDelegate(token)
    }

    override fun playSound(token: String) {
        if (!UserStore.isTokenValid(token)) {
            return
        }
        val guildId = UserStore.findGuildIdFor(token)
        Mono.justOrEmpty(Optional.ofNullable(guildId))
                .flatMap { client.getGuildById(it) }
                .flatMap {
                    val guildPlayerManager = getGuildPlayerManager(it)
                    if (guildPlayerManager.hasVoiceConnection())
                        Mono.just(it to guildPlayerManager)
                    else
                        Mono.empty()
                }
                .doOnNext {
                    audioPlayerManager.loadItemOrdered(it.first.id.asLong(), SOUND_FILE_NAME, DelegatingResultHandler(it.second.audioPlayer))
                }
                .subscribe()
    }

    private fun processMessage(event: MessageCreateEvent) {
        Mono.justOrEmpty(event.message.content)
                .flatMap { content ->
                    Flux.fromIterable(commands)
                            .filter { command -> content.startsWith(COMMAND_PREFIX + command.input) }
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

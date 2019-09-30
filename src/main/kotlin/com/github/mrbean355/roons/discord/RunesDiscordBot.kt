package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.COMMAND_PREFIX
import com.github.mrbean355.roons.discord.audio.DelegatingResultHandler
import com.github.mrbean355.roons.discord.audio.GuildPlayerManagerProvider
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional

@Component
class RunesDiscordBot(private val client: DiscordClient, private val audioPlayerManager: AudioPlayerManager,
                      private val playerManagerProvider: GuildPlayerManagerProvider, private val commands: Set<BotCommand>) {

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

    fun playSound(token: String, soundFileName: String) {
        if (!UserStore.isTokenValid(token)) {
            return
        }
        val guildId = UserStore.findGuildIdFor(token)
        Mono.justOrEmpty(Optional.ofNullable(guildId))
                .flatMap { client.getGuildById(it) }
                .flatMap {
                    val guildPlayerManager = playerManagerProvider.get(it)
                    if (guildPlayerManager.hasVoiceConnection())
                        Mono.just(it to guildPlayerManager)
                    else
                        Mono.empty()
                }
                .doOnNext {
                    audioPlayerManager.loadItemOrdered(it.first.id.asLong(), SoundStore.getFilePath(soundFileName), DelegatingResultHandler(it.second.audioPlayer))
                }
                .subscribe()
    }

    private fun processMessage(event: MessageCreateEvent) {
        Mono.justOrEmpty(event.member)
                .filter { !it.isBot }
                .flatMap { Mono.justOrEmpty(event.message.content) }
                .flatMap { content ->
                    Flux.fromIterable(commands)
                            .filter { command -> content.startsWith(COMMAND_PREFIX + command.input) }
                            .flatMap { command -> command.execute(event) }
                            .next()
                }
                .subscribe()
    }
}

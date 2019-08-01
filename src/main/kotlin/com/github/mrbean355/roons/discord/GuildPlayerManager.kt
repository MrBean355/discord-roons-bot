package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.VOLUME_DEFAULT
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.voice.AudioProvider
import discord4j.voice.VoiceConnection
import reactor.core.publisher.Mono
import java.util.Optional

/** Audio-related classes specific to a guild. */
class GuildPlayerManager(audioPlayerManager: AudioPlayerManager) {
    val audioPlayer: AudioPlayer = audioPlayerManager.createPlayer()
    val audioProvider: AudioProvider = LavaAudioProvider(audioPlayer)
    private var voiceConnection: VoiceConnection? = null

    init {
        audioPlayer.volume = VOLUME_DEFAULT
    }

    fun getVolume() = audioPlayer.volume

    fun setVolume(volume: Int) {
        audioPlayer.volume = volume
    }

    fun hasVoiceConnection() = voiceConnection != null

    fun onVoiceConnected(voiceConnection: VoiceConnection) {
        this.voiceConnection = voiceConnection
    }

    fun tryDisconnect(): Mono<Void> {
        return Mono.justOrEmpty(Optional.ofNullable(voiceConnection))
                .doOnNext {
                    it.disconnect()
                    voiceConnection = null
                }
                .then()
    }
}
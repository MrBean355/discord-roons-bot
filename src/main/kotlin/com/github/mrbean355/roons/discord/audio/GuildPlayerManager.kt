package com.github.mrbean355.roons.discord.audio

import com.github.mrbean355.roons.VOLUME_DEFAULT
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.core.`object`.entity.Guild
import discord4j.voice.AudioProvider
import discord4j.voice.VoiceConnection
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

@Component
class GuildPlayerManagerProvider(private val audioPlayerManager: AudioPlayerManager) {
    private val guildPlayerManagers: MutableMap<Long, GuildPlayerManager> = ConcurrentHashMap()

    fun get(guild: Guild): GuildPlayerManager {
        return guildPlayerManagers.getOrPut(guild.id.asLong()) {
            GuildPlayerManager(audioPlayerManager)
        }
    }
}

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
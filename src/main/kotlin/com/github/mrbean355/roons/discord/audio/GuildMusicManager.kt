package com.github.mrbean355.roons.discord.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

const val DEFAULT_VOLUME = 25
private const val MIN_VOLUME = 0
private const val MAX_VOLUME = 100

class GuildMusicManager(
    manager: AudioPlayerManager
) {
    private val player = manager.createPlayer()
    val scheduler = TrackScheduler(player)

    init {
        player.addListener(scheduler)
    }

    fun getSendHandler(): AudioPlayerSendHandler {
        return AudioPlayerSendHandler(player)
    }
}

fun Int.coerceVolume(): Int {
    return coerceAtLeast(MIN_VOLUME).coerceAtMost(MAX_VOLUME)
}

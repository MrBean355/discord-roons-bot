package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

private const val DEFAULT_VOLUME = 25
private const val MIN_VOLUME = 0
private const val MAX_VOLUME = 100

class GuildMusicManager(manager: AudioPlayerManager) {
    private val player = manager.createPlayer()
    val scheduler = TrackScheduler(player)

    init {
        player.volume = DEFAULT_VOLUME
        player.addListener(scheduler)
    }

    fun getSendHandler(): AudioPlayerSendHandler {
        return AudioPlayerSendHandler(player)
    }

    fun getVolume(): Int {
        return player.volume
    }

    fun setVolume(volume: Int) {
        player.volume = volume
    }
}

fun Int.coerceVolume(): Int {
    return coerceAtLeast(MIN_VOLUME).coerceAtMost(MAX_VOLUME)
}

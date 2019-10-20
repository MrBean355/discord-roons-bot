package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

private const val SOUND_VOLUME = 50

class GuildMusicManager(manager: AudioPlayerManager) {
    private val player = manager.createPlayer()
    val scheduler = TrackScheduler(player)

    init {
        player.volume = SOUND_VOLUME
        player.addListener(scheduler)
    }

    fun getSendHandler(): AudioPlayerSendHandler {
        return AudioPlayerSendHandler(player)
    }
}
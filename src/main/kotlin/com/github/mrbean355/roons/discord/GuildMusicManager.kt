package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

const val DEFAULT_VOLUME = 25
private const val MIN_VOLUME = 0
private const val MAX_VOLUME = 100

class GuildMusicManager(private val guildId: String, manager: AudioPlayerManager, private val discordBotSettingsRepository: DiscordBotSettingsRepository) {
    private val player = manager.createPlayer()
    val scheduler = TrackScheduler(player)

    init {
        player.addListener(scheduler)
    }

    fun getSendHandler(): AudioPlayerSendHandler {
        return AudioPlayerSendHandler(player)
    }

    fun getMasterVolume(): Int {
        return discordBotSettingsRepository.loadSettings(guildId).volume
    }

    fun setMasterVolume(volume: Int) {
        discordBotSettingsRepository.loadSettings(guildId).let {
            it.volume = volume
            discordBotSettingsRepository.save(it)
        }
    }
}

fun Int.coerceVolume(): Int {
    return coerceAtLeast(MIN_VOLUME).coerceAtMost(MAX_VOLUME)
}

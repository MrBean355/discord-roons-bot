package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

private const val DEFAULT_VOLUME = 25
private const val MIN_VOLUME = 0
private const val MAX_VOLUME = 100

class GuildMusicManager(private val guildId: String, manager: AudioPlayerManager, private val discordBotSettingsRepository: DiscordBotSettingsRepository) {
    private val player = manager.createPlayer()
    val scheduler = TrackScheduler(player)

    init {
        player.volume = loadSettings().volume
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
        loadSettings().let {
            it.volume = volume
            discordBotSettingsRepository.save(it)
        }
    }

    private fun loadSettings(): DiscordBotSettings {
        val settings = discordBotSettingsRepository.findOneByGuildId(guildId)
        if (settings != null) {
            return settings
        }
        return discordBotSettingsRepository.save(DiscordBotSettings(0, guildId, DEFAULT_VOLUME))
    }
}

fun Int.coerceVolume(): Int {
    return coerceAtLeast(MIN_VOLUME).coerceAtMost(MAX_VOLUME)
}

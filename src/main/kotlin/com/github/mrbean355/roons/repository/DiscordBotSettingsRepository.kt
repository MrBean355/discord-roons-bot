package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.discord.DEFAULT_VOLUME
import org.springframework.data.repository.CrudRepository

interface DiscordBotSettingsRepository : CrudRepository<DiscordBotSettings, Int> {
    fun findOneByGuildId(guildId: String): DiscordBotSettings?
}

fun DiscordBotSettingsRepository.loadSettings(guildId: String): DiscordBotSettings {
    val settings = findOneByGuildId(guildId)
    if (settings != null) {
        return settings
    }
    return save(DiscordBotSettings(0, guildId, DEFAULT_VOLUME, null))
}

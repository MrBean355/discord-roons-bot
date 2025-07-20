package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.discord.audio.DEFAULT_VOLUME
import jakarta.transaction.Transactional
import org.springframework.data.repository.CrudRepository

interface DiscordBotSettingsRepository : CrudRepository<DiscordBotSettings, Int> {

    fun findOneByGuildId(guildId: String): DiscordBotSettings?

    @Transactional
    fun deleteByGuildId(guildId: String): Int

}

fun DiscordBotSettingsRepository.loadSettings(guildId: String): DiscordBotSettings {
    val settings = findOneByGuildId(guildId)
    if (settings != null) {
        return settings
    }
    return save(DiscordBotSettings(0, guildId, DEFAULT_VOLUME, null, null))
}

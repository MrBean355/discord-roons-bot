package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotSettings
import org.springframework.data.repository.CrudRepository

interface DiscordBotSettingsRepository : CrudRepository<DiscordBotSettings, Int> {
    fun findOneByGuildId(guildId: String): DiscordBotSettings?
}
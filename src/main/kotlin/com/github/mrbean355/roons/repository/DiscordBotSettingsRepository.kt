package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotSettings
import jakarta.transaction.Transactional
import org.springframework.data.repository.CrudRepository

interface DiscordBotSettingsRepository : CrudRepository<DiscordBotSettings, Int> {

    fun findOneByGuildId(guildId: String): DiscordBotSettings?

    @Transactional
    fun deleteByGuildId(guildId: String): Int

}


package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotSettings
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

interface DiscordBotSettingsRepository : CrudRepository<DiscordBotSettings, Int> {

    fun findOneByGuildId(guildId: String): DiscordBotSettings?

    @Transactional
    fun deleteByGuildId(guildId: String): Int

}


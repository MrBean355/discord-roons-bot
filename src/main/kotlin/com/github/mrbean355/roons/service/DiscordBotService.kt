package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.discord.audio.DEFAULT_VOLUME
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscordBotService(
    private val discordBotUserRepository: DiscordBotUserRepository,
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) {

    @Transactional(readOnly = true)
    fun findUserByToken(token: String): DiscordBotUser? {
        return discordBotUserRepository.findOneByToken(token)
    }

    @Transactional
    fun cleanUpGuild(guildId: String) {
        discordBotUserRepository.deleteByGuildId(guildId)
        discordBotSettingsRepository.deleteByGuildId(guildId)
    }

    @Transactional
    fun loadSettings(guildId: String): DiscordBotSettings {
        val settings = discordBotSettingsRepository.findOneByGuildId(guildId)
        if (settings != null) {
            return settings
        }
        return discordBotSettingsRepository.save(DiscordBotSettings(0, guildId, DEFAULT_VOLUME, null, null))
    }

    @Transactional
    fun saveSettings(settings: DiscordBotSettings): DiscordBotSettings {
        return discordBotSettingsRepository.save(settings)
    }
}

package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotUser
import jakarta.transaction.Transactional
import org.springframework.data.repository.CrudRepository

interface DiscordBotUserRepository : CrudRepository<DiscordBotUser, Int> {

    fun findOneByDiscordUserIdAndGuildId(userId: String, guildId: String): DiscordBotUser?

    fun findOneByToken(token: String): DiscordBotUser?

    @Transactional
    fun deleteByGuildId(guildId: String): Int

}
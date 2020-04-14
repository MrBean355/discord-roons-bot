package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotUser
import org.springframework.data.repository.CrudRepository
import javax.transaction.Transactional

interface DiscordBotUserRepository : CrudRepository<DiscordBotUser, Int> {

    fun findOneByDiscordUserIdAndGuildId(userId: String, guildId: String): DiscordBotUser?

    fun findOneByToken(token: String): DiscordBotUser?

    @Transactional
    fun deleteByGuildId(guildId: String): Int

}
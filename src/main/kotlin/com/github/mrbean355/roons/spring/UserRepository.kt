package com.github.mrbean355.roons.spring

import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Int> {

    fun findOneByUserIdAndGuildId(userId: String, guildId: String): User?

    fun findOneByToken(token: String): User?
}
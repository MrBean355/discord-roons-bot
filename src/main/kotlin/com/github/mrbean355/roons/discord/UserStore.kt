package com.github.mrbean355.roons.discord

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.util.Snowflake
import java.io.File
import java.util.UUID

/** Stores users' ID & guild ID as the key, and their token as the value. */
object UserStore {
    private val cache: MutableMap<String, String> = mutableMapOf()

    init {
        synchronized(this) {
            cache.clear()
            cache.putAll(file().readLines().map { entry ->
                val s = entry.split('=')
                s[0] to s[1]
            })
        }
    }

    /** @return `true` if the token is associated with a user, `false` otherwise. */
    fun isTokenValid(token: String): Boolean {
        if (token.isBlank()) {
            return false
        }
        return synchronized(this) {
            cache.containsValue(token)
        }
    }

    /** @return the guild ID corresponding to the token if found, `null` otherwise. */
    fun findGuildIdFor(token: String): Snowflake? {
        if (token.isBlank()) {
            return null
        }
        return synchronized(this) {
            if (cache.containsValue(token)) {
                val key = cache.filterValues { it == token }.keys.single().split(',')
                Snowflake.of(key[1])
            } else {
                null
            }
        }
    }

    /** @return the user's token, creating one if they don't have a token. */
    fun getOrCreate(user: User, guild: Guild): String {
        return synchronized(this) {
            val key = "${user.id.asString()},${guild.id.asString()}"
            var token = cache[key]
            if (token == null) {
                token = UUID.randomUUID().toString()
                cache[key] = token
                file().appendText("$key=$token\n")
            }
            token
        }
    }

    private fun file(): File {
        val file = File("tokens")
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }
}
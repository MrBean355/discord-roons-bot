package com.github.mrbean355.roons.discord

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.util.Snowflake
import java.io.File
import java.util.UUID

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

    fun userExists(token: String): Boolean {
        if (token.isEmpty()) {
            return false
        }
        return synchronized(this) {
            cache.containsValue(token)
        }
    }

    fun findUserId(token: String): Pair<Snowflake, Snowflake>? {
        if (token.isEmpty()) {
            return null
        }
        return synchronized(this) {
            if (cache.containsValue(token)) {
                val key = cache.filterValues { it == token }.keys.single().split(',')
                Snowflake.of(key[0]) to Snowflake.of(key[1])
            } else {
                null
            }
        }
    }

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
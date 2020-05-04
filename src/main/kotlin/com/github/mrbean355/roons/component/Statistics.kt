package com.github.mrbean355.roons.component

import org.springframework.stereotype.Component

@Component
class Statistics {
    private val stats = mutableMapOf<Type, Long>()

    @Synchronized
    fun increment(type: Type) {
        stats[type] = stats.getOrDefault(type, 0) + 1
    }

    @Synchronized
    fun take(type: Type): Long {
        val result = stats.getOrDefault(type, 0)
        stats.remove(type)
        return result
    }

    fun isEmpty(): Boolean {
        return stats.isEmpty()
    }

    enum class Type {
        DISCORD_SOUNDS,
        DISCORD_COMMANDS,
        NEW_USERS
    }
}

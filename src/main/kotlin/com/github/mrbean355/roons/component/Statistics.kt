package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.telegram.TelegramNotifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Statistics(private val telegramNotifier: TelegramNotifier) {
    private val stats = mutableMapOf<Type, Long>()

    @Synchronized
    fun increment(type: Type) {
        stats[type] = stats.getOrDefault(type, 0) + 1
    }

    @Synchronized
    private fun take(type: Type): Long {
        val result = stats.getOrDefault(type, 0)
        stats.remove(type)
        return result
    }

    @Scheduled(initialDelayString = "P1D", fixedRateString = "P1D")
    fun sendStatisticsNotification() {
        telegramNotifier.sendMessage("""
            📈 <b>Stats from the last day</b>:
            Discord sounds: ${take(Type.DISCORD_SOUNDS)}
            Discord commands: ${take(Type.DISCORD_COMMANDS)}
            New app users: ${take(Type.NEW_USERS)}
        """.trimIndent())
    }

    enum class Type {
        DISCORD_SOUNDS,
        DISCORD_COMMANDS,
        NEW_USERS
    }
}

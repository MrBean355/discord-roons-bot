/*
 * Copyright 2021 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

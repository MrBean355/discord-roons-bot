/*
 * Copyright 2022 Michael Johnston
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

package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.DiscordBotSettings
import com.github.mrbean355.roons.discord.audio.DEFAULT_VOLUME
import jakarta.transaction.Transactional
import org.springframework.data.repository.CrudRepository

interface DiscordBotSettingsRepository : CrudRepository<DiscordBotSettings, Int> {

    fun findOneByGuildId(guildId: String): DiscordBotSettings?

    @Transactional
    fun deleteByGuildId(guildId: String): Int

}

fun DiscordBotSettingsRepository.loadSettings(guildId: String): DiscordBotSettings {
    val settings = findOneByGuildId(guildId)
    if (settings != null) {
        return settings
    }
    return save(DiscordBotSettings(0, guildId, DEFAULT_VOLUME, null, null))
}

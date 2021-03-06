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

package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

const val DEFAULT_VOLUME = 25
private const val MIN_VOLUME = 0
private const val MAX_VOLUME = 100

class GuildMusicManager(private val guildId: String, manager: AudioPlayerManager, private val discordBotSettingsRepository: DiscordBotSettingsRepository) {
    private val player = manager.createPlayer()
    val scheduler = TrackScheduler(player)

    init {
        player.addListener(scheduler)
    }

    fun getSendHandler(): AudioPlayerSendHandler {
        return AudioPlayerSendHandler(player)
    }

    fun getMasterVolume(): Int {
        return discordBotSettingsRepository.loadSettings(guildId).volume
    }

    fun setMasterVolume(volume: Int) {
        discordBotSettingsRepository.loadSettings(guildId).let {
            it.volume = volume
            discordBotSettingsRepository.save(it)
        }
    }
}

fun Int.coerceVolume(): Int {
    return coerceAtLeast(MIN_VOLUME).coerceAtMost(MAX_VOLUME)
}

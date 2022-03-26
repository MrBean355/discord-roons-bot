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

package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import com.github.mrbean355.roons.telegram.TelegramNotifier
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DiscordBot(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository,
    private val soundStore: SoundStore,
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger,
    private val bot: JDA
) {

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager> = mutableMapOf()
    private val activities = listOf(
        Activity.playing("with the roons"),
        Activity.playing("with your mom"),
        Activity.listening("Bulldog yelling"),
        Activity.listening("Bulldog malding"),
        Activity.listening("STUN HIM!"),
        Activity.listening("COVER THE EXITS!"),
        Activity.watching("a god gamer"),
        Activity.watching("bad game design"),
        Activity.watching("Bulldog being stomped"),
        Activity.competing("Cute Tales"),
    )

    init {
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    @Scheduled(fixedRate = 86_400_000) // daily
    fun updateBotPresence() {
        bot.presence.setPresence(OnlineStatus.ONLINE, activities.random())
    }

    /** Try to play the given [soundFileName] in a guild. */
    fun playSound(discordBotUser: DiscordBotUser, soundFileName: String, volume: Int, rate: Int): Boolean {
        val guild = bot.getGuildById(discordBotUser.guildId) ?: return false
        val file = soundStore.getFile(soundFileName) ?: return false
        val masterVolume = discordBotSettingsRepository.loadSettings(discordBotUser.guildId).volume
        val finalVolume = (volume * masterVolume) / 100
        return playSound(guild, file.absolutePath, finalVolume, rate)
    }

    /** Disconnect from voice channels when shutting down. */
    fun shutdown() = runBlocking(IO) {
        bot.presence.setStatus(OnlineStatus.OFFLINE)
        val connectedGuilds = bot.guilds.filter { it.audioManager.isConnected }
        supervisorScope {
            connectedGuilds.forEach { guild ->
                launch {
                    val settings = discordBotSettingsRepository.loadSettings(guild.id)
                    val currentVoiceChannel = guild.selfMember.voiceState?.channel?.id
                    discordBotSettingsRepository.save(settings.copy(lastChannel = currentVoiceChannel))
                    guild.audioManager.closeAudioConnection()
                }
            }
        }
        telegramNotifier.sendPrivateMessage("⚙️ <b>Shutting down</b>:\nDisconnected from <b>${connectedGuilds.size}</b> voice channels.")
    }

    fun getGuilds(): List<Guild> = bot.guilds

    fun getGuildById(id: String): Guild? {
        return bot.getGuildById(id)
    }

    /** @return a guild-specific [GuildMusicManager]. */
    private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
        return synchronized(this) {
            val manager = musicManagers.getOrPut(guild.idLong) { GuildMusicManager(guild.id, playerManager, discordBotSettingsRepository) }
            guild.audioManager.sendingHandler = manager.getSendHandler()
            manager
        }
    }

    private fun playSound(guild: Guild, filePath: String, volume: Int, rate: Int): Boolean {
        if (!guild.audioManager.isConnected) {
            logger.warn("Tried to play sound while not in voice channel.")
            return false
        }
        val manager = getGuildAudioPlayer(guild)
        playerManager.loadItemOrdered(manager, filePath, object : AudioLoadResultHandler {

            override fun trackLoaded(track: AudioTrack) {
                manager.scheduler.queue(track, volume, rate)
            }

            override fun playlistLoaded(playlist: AudioPlaylist?) {
                logger.warn("Somehow loaded a playlist; ignoring.")
            }

            override fun noMatches() {
                logger.error("No matches found for '$filePath'.")
            }

            override fun loadFailed(exception: FriendlyException?) {
                logger.error("Failed to load track '$filePath': $exception")
            }
        })
        return true
    }
}
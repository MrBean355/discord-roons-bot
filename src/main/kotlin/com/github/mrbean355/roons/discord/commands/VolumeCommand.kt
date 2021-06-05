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

package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.discord.coerceVolume
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.springframework.stereotype.Component

private const val OPTION_SET_VOLUME = "new-volume"

@Component
class VolumeCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val name get() = "volume"
    override val description get() = "Get or set the bot's volume when playing sounds in voice channels."

    override fun build(commandData: CommandData) =
        commandData.addOption(OptionType.INTEGER, OPTION_SET_VOLUME, "Set a new volume level.")

    override fun process(event: SlashCommandEvent) {
        val guild = event.guild ?: return
        val settings = discordBotSettingsRepository.loadSettings(guild.id)
        val newVolume = event.getOption(OPTION_SET_VOLUME)?.asLong?.toInt()?.coerceVolume()

        if (newVolume == null) {
            event.queueEphemeralReply("My volume is at `${settings.volume}%` :loud_sound:")
        } else {
            discordBotSettingsRepository.save(settings.copy(volume = newVolume))
            event.queueEphemeralReply("My volume has been set to `${newVolume}%` :ok_hand:")
        }
    }
}
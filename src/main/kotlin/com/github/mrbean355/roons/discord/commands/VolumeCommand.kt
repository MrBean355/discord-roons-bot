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

package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.discord.audio.coerceVolume
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

private const val OPTION_SET_VOLUME = "new-volume"

@Component
class VolumeCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val name get() = "volume"
    override val description get() = "Get or set the bot's volume when playing sounds in voice channels."

    override fun buildCommand(commandData: SlashCommandData) = commandData
        .addOption(OptionType.INTEGER, OPTION_SET_VOLUME, "Set a new volume level.")

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val newVolume = event.getOption(OPTION_SET_VOLUME)?.asLong?.toInt()?.coerceVolume()

        if (newVolume == null) {
            event.reply(getVolume(member)).setEphemeral(true).queue()
        } else {
            event.reply(setVolume(member, newVolume)).queue()
        }
    }

    private fun getVolume(member: Member): String {
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        return "My volume is at `${settings.volume}%`."
    }

    private fun setVolume(member: Member, newVolume: Int): String {
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        discordBotSettingsRepository.save(settings.copy(volume = newVolume))
        return "My volume has been set to `${newVolume}%`."
    }
}
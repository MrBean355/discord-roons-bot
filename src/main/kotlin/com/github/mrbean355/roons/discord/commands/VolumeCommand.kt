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
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.springframework.stereotype.Component

private const val OPTION_SET_VOLUME = "new-volume"

@Component
class VolumeCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val legacyName get() = "volume"
    override val name get() = "volume"
    override val description get() = "Get or set the bot's volume when playing sounds in voice channels."

    override fun buildSlashCommand(commandData: CommandData) = commandData
        .addOption(OptionType.INTEGER, OPTION_SET_VOLUME, "Set a new volume level.")

    override fun handleMessageCommand(context: MessageCommandContext) {
        val newVolumeArg = context.arguments.firstOrNull()
        if (newVolumeArg != null) {
            val newVolume = newVolumeArg.toIntOrNull()
            if (newVolume != null) {
                context.reply(setVolume(context.member, newVolume))
            } else {
                context.reply("I'm not sure what you mean :disappointed:\nExample usage: `!volume 50`")
            }
        } else {
            context.reply(getVolume(context.member))
        }
    }

    override fun handleSlashCommand(context: SlashCommandContext) {
        val newVolume = context.getOption(OPTION_SET_VOLUME)?.asLong?.toInt()?.coerceVolume()

        if (newVolume == null) {
            context.reply(getVolume(context.member))
        } else {
            context.reply(setVolume(context.member, newVolume))
        }
    }

    private fun getVolume(member: Member): String {
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        return "My volume is at `${settings.volume}%` :loud_sound:"
    }

    private fun setVolume(member: Member, newVolume: Int): String {
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        discordBotSettingsRepository.save(settings.copy(volume = newVolume))
        return "My volume has been set to `${newVolume}%` :ok_hand:"
    }
}
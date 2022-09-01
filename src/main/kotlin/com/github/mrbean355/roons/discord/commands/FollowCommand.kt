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

import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class FollowCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val name get() = "follow"
    override val description get() = "Join & leave the same voice channels (in this server) as you."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        val followedUser = settings.followedUser

        if (followedUser == member.id) {
            event.reply("I'm already following you.").setEphemeral(true).queue()
            return
        }

        discordBotSettingsRepository.save(settings.copy(followedUser = member.id))

        member.voiceState?.channel?.let {
            member.guild.audioManager.openAudioConnection(it)
        }

        val followedMember = followedUser?.let { member.guild.retrieveMemberById(it).complete() }
        val reply = buildString {
            append("I'm now following ${member.asMention}")
            if (followedMember != null) {
                append(' ').append("instead of ${followedMember.asMention}")
            }
            append('.')
        }
        event.reply(reply).queue()
    }
}
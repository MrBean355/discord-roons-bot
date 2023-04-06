/*
 * Copyright 2023 Michael Johnston
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

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NewMagicNumberCommand(
    private val discordBotUserRepository: DiscordBotUserRepository
) : BotCommand {

    override val name get() = CommandName
    override val description get() = "Create a new \"magic number\" for use in the desktop app."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val botUser = discordBotUserRepository.findOneByDiscordUserIdAndGuildId(member.id, member.guild.id)
            ?: discordBotUserRepository.save(DiscordBotUser(0, member.id, member.guild.id, UUID.randomUUID().toString()))

        val newToken = UUID.randomUUID().toString()
        discordBotUserRepository.save(botUser.copy(token = newToken))

        event.reply(
            "Here's your **new** magic number:\n" +
                    "```$newToken```\n" +
                    "**Please keep it secret!** Anyone who has this magic number will be able to play sounds through the bot in your server."
        ).setEphemeral(true).queue()
    }

    companion object {
        const val CommandName = "new-magic-number"
    }
}
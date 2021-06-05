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

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.springframework.stereotype.Component
import java.util.UUID

// TODO: command to reset magic number
@Component
class MagicCommand(
    private val discordBotUserRepository: DiscordBotUserRepository
) : BotCommand {

    override val name get() = "magic"
    override val description get() = "Get your \"magic number\" for communicating with the bot."

    override fun process(event: SlashCommandEvent) {
        val userId = event.member?.id ?: return
        val guildId = event.guild?.id ?: return

        val botUser = discordBotUserRepository.findOneByDiscordUserIdAndGuildId(userId, guildId)
            ?: discordBotUserRepository.save(DiscordBotUser(0, userId, guildId, UUID.randomUUID().toString()))

        event.queueEphemeralReply("Here's your magic number:\n```${botUser.token}```\nDon't share this with anyone!")
    }
}
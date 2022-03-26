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

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import java.util.UUID

private const val COMMAND_GET = "get"
private const val COMMAND_NEW = "new"

@Component
class MagicNumberCommand(
    private val discordBotUserRepository: DiscordBotUserRepository
) : BotCommand {

    override val legacyName get() = "magic"
    override val name get() = "magicnumber"
    override val description get() = "Get or recreate your \"magic number\" for communicating with the bot."

    override fun buildSlashCommand(commandData: CommandData) = commandData
        .addSubcommands(
            SubcommandData(COMMAND_GET, "Get your current magic number."),
            SubcommandData(COMMAND_NEW, "Create a new magic number in case your previous one was leaked.")
        )

    override fun handleMessageCommand(context: MessageCommandContext) {
        val message = if (context.arguments.firstOrNull() == COMMAND_NEW) {
            createMagicNumber(context.member)
        } else {
            getMagicNumber(context.member)
        }
        context.reply(message, sensitive = true)
    }

    override fun handleSlashCommand(context: SlashCommandContext) {
        val message = if (context.subcommandName == COMMAND_NEW) {
            createMagicNumber(context.member)
        } else {
            getMagicNumber(context.member)
        }
        context.reply(message, sensitive = true)
    }

    private fun getMagicNumber(member: Member): String {
        val botUser = discordBotUserRepository.findOneByDiscordUserIdAndGuildId(member.id, member.guild.id)
            ?: discordBotUserRepository.save(DiscordBotUser(0, member.id, member.guild.id, MagicNumber()))

        return "Here's your magic number:\n" +
                "```${botUser.token}```\n" +
                "Please keep it secret! Anyone who has this magic number will be able to play sounds through the bot in your server."
    }

    private fun createMagicNumber(member: Member): String {
        val botUser = discordBotUserRepository.findOneByDiscordUserIdAndGuildId(member.id, member.guild.id)
            ?: discordBotUserRepository.save(DiscordBotUser(0, member.id, member.guild.id, MagicNumber()))

        val newToken = MagicNumber()
        discordBotUserRepository.save(botUser.copy(token = newToken))

        return "Here's your **new** magic number:\n" +
                "```$newToken```\n" +
                "Please keep it secret! Anyone who has this magic number will be able to play sounds through the bot in your server."
    }

    @Suppress("FunctionName")
    private fun MagicNumber(): String = UUID.randomUUID().toString()

}
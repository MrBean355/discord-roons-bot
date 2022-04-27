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

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.commands.build.CommandData

/** A command that users can type in a guild channel to interact with the bot. */
sealed interface BotCommand {

    /** Name used for slash commands. */
    val name: String

    /** Description used for slash commands. */
    val description: String

    /** Optionally build upon the [CommandData] object. */
    fun buildSlashCommand(commandData: CommandData): CommandData = commandData

    /** Handle the received slash command (e.g. /command). */
    fun handleSlashCommand(context: SlashCommandContext)

}

/** Simple command which doesn't need additional command context. */
abstract class BasicCommand : BotCommand {

    /** Handle the received command. Could be a message or slash command. */
    abstract fun handleCommand(member: Member, reply: CommandReply)

    final override fun handleSlashCommand(context: SlashCommandContext) =
        handleCommand(context.member, context.reply)
}
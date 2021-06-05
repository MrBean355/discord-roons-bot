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

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction

/**
 * A slash command that users can type to interact with the bot.
 */
sealed interface BotCommand {
    val name: String
    val description: String

    /** Optionally build upon the [CommandData] object. */
    fun build(commandData: CommandData): CommandData = commandData

    /** Handle the command when it is received. */
    fun process(event: SlashCommandEvent)

}

@Suppress("NOTHING_TO_INLINE")
inline fun Interaction.ephemeralReply(content: String): ReplyAction =
    reply(content).setEphemeral(true)

@Suppress("NOTHING_TO_INLINE")
inline fun Interaction.queueEphemeralReply(content: String): Unit =
    ephemeralReply(content).queue()

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
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping

/** Context of a slash command. */
class SlashCommandContext(
    private val event: SlashCommandEvent
) {

    val member: Member get() = event.member!!

    val reply = CommandReply(event)

    /** Name of the sub-command if applicable. */
    val subcommandName: String? get() = event.subcommandName

    /** Finds the first option with the given name. */
    fun getOption(name: String): OptionMapping? = event.getOption(name)

}

/** Sends a reply to the slash command. */
class CommandReply(
    private val event: SlashCommandEvent
) {

    operator fun invoke(text: String, sensitive: Boolean = false) {
        event.reply(text).setEphemeral(sensitive).queue()
    }
}
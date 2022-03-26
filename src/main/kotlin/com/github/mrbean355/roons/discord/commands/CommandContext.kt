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
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.commands.OptionMapping

sealed class CommandContext<E : Event>(protected val event: E) {

    /** Member that initiated the command. */
    abstract val member: Member

    val reply = CommandReply(event)
}

/** Context of a message command (starts with '!'). */
class MessageCommandContext(event: MessageReceivedEvent) : CommandContext<MessageReceivedEvent>(event) {

    override val member: Member get() = event.member!!

    /** Command arguments (excluding the command name). */
    val arguments: List<String> by lazy {
        event.message.contentRaw.trim()
            .drop(1) // '!' prefix
            .split(' ')
            .filter { it.isNotBlank() }
            .drop(1) // command name
    }
}

/** Context of a slash command. */
class SlashCommandContext(event: SlashCommandEvent) : CommandContext<SlashCommandEvent>(event) {

    override val member: Member get() = event.member!!

    /** Name of the sub-command if applicable. */
    val subcommandName: String? get() = event.subcommandName

    /** Finds the first option with the given name. */
    fun getOption(name: String): OptionMapping? = event.getOption(name)

}

/** Sends the applicable reply to a message or slash command. */
class CommandReply(private val event: Event) {

    operator fun invoke(text: String, sensitive: Boolean = false) {
        when (event) {
            is MessageReceivedEvent -> if (sensitive) event.sendPrivateMessage(text) else event.typeReply(decorate(text))
            is SlashCommandEvent -> event.queueEphemeralReply(text)
            else -> error("Unsupported event type: ${event::class}")
        }
    }

    private fun decorate(text: String) = "$text\n*⭐ Try out the fancy slash commands! Type `/` to see what's available.*"

}

private fun MessageReceivedEvent.typeReply(text: String): Unit =
    channel.sendTyping().queue {
        channel.sendMessage(text).queue()
    }

private fun MessageReceivedEvent.sendPrivateMessage(text: String): Unit =
    author.openPrivateChannel().queue {
        it.sendMessage(text).queue {
            message.addReaction("✅").queue()
        }
    }

private fun Interaction.queueEphemeralReply(content: String): Unit =
    reply(content).setEphemeral(true).queue()
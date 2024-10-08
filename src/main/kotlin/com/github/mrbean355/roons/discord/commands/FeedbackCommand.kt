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

import com.github.mrbean355.roons.telegram.TelegramNotifier
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

private const val OPTION_COMMENTS = "comments"

@Component
class FeedbackCommand(
    private val telegramNotifier: TelegramNotifier
) : BotCommand {

    override val name get() = "feedback"
    override val description get() = "Send some feedback to the developer."

    override fun buildCommand(commandData: SlashCommandData) = commandData
        .addOption(OptionType.STRING, OPTION_COMMENTS, "All comments & suggestions are welcome!", true)

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val comments = event.getOption(OPTION_COMMENTS)?.asString.orEmpty()
        telegramNotifier.sendPrivateMessage("📋 <b>Feedback received</b>\nComments: $comments")
        event.reply("Thank you for your feedback.").setEphemeral(true).queue()
    }
}
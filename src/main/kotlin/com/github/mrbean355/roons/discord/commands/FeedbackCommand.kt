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

import com.github.mrbean355.roons.telegram.TelegramNotifier
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.springframework.stereotype.Component

private const val OPTION_COMMENTS = "comments"

@Component
class FeedbackCommand(
    private val telegramNotifier: TelegramNotifier
) : BotCommand {

    override val legacyName get() = "feedback"
    override val name get() = "feedback"
    override val description get() = "Provide the developer with your feedback."

    override fun buildSlashCommand(commandData: CommandData) = commandData
        .addOption(OptionType.STRING, OPTION_COMMENTS, "Your thoughts on the Admiral Bulldog sound pack", true)

    override fun handleMessageCommand(context: MessageCommandContext) {
        if (context.arguments.isNotEmpty()) {
            val comments = context.arguments.joinToString(separator = " ")
            context.reply(feedback(comments))
        }
    }

    override fun handleSlashCommand(context: SlashCommandContext) {
        val comments = context.getOption(OPTION_COMMENTS)?.asString.orEmpty()
        context.reply(feedback(comments))
    }

    private fun feedback(comments: String): String {
        telegramNotifier.sendPrivateMessage("<b>Feedback received</b>\nComments: $comments")
        return "Thank you."
    }
}
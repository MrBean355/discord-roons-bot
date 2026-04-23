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
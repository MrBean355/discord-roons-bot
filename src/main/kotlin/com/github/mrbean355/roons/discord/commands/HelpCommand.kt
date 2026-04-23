package com.github.mrbean355.roons.discord.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

private const val MESSAGE = "Interact with the bot using Discord's slash commands.\n" +
        "Just type slash (`/`) in a text channel and a list of available commands will pop up!\n" +
        "For more info or to log a bug, please visit: https://github.com/MrBean355/admiralbulldog-sounds/wiki/Discord-Bot"

@Component
class HelpCommand : BotCommand {

    override val name get() = "help"
    override val description get() = "Get some help with using the bot."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        event.reply(MESSAGE).setEphemeral(true).queue()
    }
}
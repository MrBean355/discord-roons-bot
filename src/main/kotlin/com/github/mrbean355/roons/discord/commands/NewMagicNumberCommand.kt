package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.service.DiscordBotService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class NewMagicNumberCommand(
    private val discordBotService: DiscordBotService
) : BotCommand {

    override val name get() = CommandName
    override val description get() = "Create a new \"magic number\" for use in the desktop app."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val newToken = discordBotService.generateNewUserToken(member.id, member.guild.id)

        event.reply(
            "Here's your **new** magic number:\n" +
                    "```$newToken```\n" +
                    "**Please keep it secret!** Anyone who has this magic number will be able to play sounds through the bot in your server."
        ).setEphemeral(true).queue()
    }

    companion object {
        const val CommandName = "new-magic-number"
    }
}
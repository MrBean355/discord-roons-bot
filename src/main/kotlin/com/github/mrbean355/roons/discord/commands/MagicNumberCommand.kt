package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.service.DiscordBotService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class MagicNumberCommand(
    private val discordBotService: DiscordBotService
) : BotCommand {

    override val name get() = "magic-number"
    override val description get() = "Check your \"magic number\" for use in the desktop app."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val token = discordBotService.getOrCreateUserToken(member.id, member.guild.id)

        event.reply(
            "Here's your magic number:\n" +
                    "```$token```\n" +
                    "**Please keep it secret!** Anyone who has this magic number will be able to play sounds through the bot in your server.\n" +
                    "Use `/${NewMagicNumberCommand.CommandName}` to create a new one if it was leaked."
        ).setEphemeral(true).queue()
    }
}
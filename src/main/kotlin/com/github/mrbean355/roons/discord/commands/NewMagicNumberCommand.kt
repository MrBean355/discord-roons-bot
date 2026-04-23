package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.DiscordBotUser
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NewMagicNumberCommand(
    private val discordBotUserRepository: DiscordBotUserRepository
) : BotCommand {

    override val name get() = CommandName
    override val description get() = "Create a new \"magic number\" for use in the desktop app."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val botUser = discordBotUserRepository.findOneByDiscordUserIdAndGuildId(member.id, member.guild.id)
            ?: discordBotUserRepository.save(DiscordBotUser(0, member.id, member.guild.id, UUID.randomUUID().toString()))

        val newToken = UUID.randomUUID().toString()
        discordBotUserRepository.save(botUser.copy(token = newToken))

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
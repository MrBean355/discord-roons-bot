package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class UnfollowCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val name get() = "unfollow"
    override val description get() = "Stop following you when you join & leave voice channels."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        val followedUser = settings.followedUser

        if (followedUser != null) {
            discordBotSettingsRepository.save(settings.copy(followedUser = null))
            val user = member.guild.retrieveMemberById(followedUser).complete()?.asMention ?: "someone"
            event.reply("I've stopped following $user.").queue()
        } else {
            event.reply("I'm not following anyone at the moment.").setEphemeral(true).queue()
        }
    }
}
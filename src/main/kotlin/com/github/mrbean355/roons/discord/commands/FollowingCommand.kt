package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class FollowingCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val name get() = "following"
    override val description get() = "Check who is being followed."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val followedUser = discordBotSettingsRepository.loadSettings(guild.id).followedUser

        if (followedUser == null) {
            event.reply("I'm not following anyone at the moment.").setEphemeral(true).queue()
        } else {
            guild.retrieveMemberById(followedUser).queue { member ->
                event.reply("I'm following ${member.effectiveName}.").setEphemeral(true).queue()
            }
        }
    }
}
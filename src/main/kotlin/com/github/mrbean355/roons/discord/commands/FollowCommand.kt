package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.service.DiscordBotService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class FollowCommand(
    private val discordBotService: DiscordBotService
) : BotCommand {

    override val name get() = "follow"
    override val description get() = "Join & leave the same voice channels (in this server) as you."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val settings = discordBotService.loadSettings(member.guild.id)
        val followedUser = settings.followedUser
        if (followedUser == member.id) {
            event.reply("I'm already following you.").setEphemeral(true).queue()
            return
        }

        discordBotService.saveSettings(settings.copy(followedUser = member.id))

        member.voiceState?.channel?.let {
            member.guild.audioManager.openAudioConnection(it)
        }

        val followedMember = followedUser?.let { member.guild.findMember(it) }
        val reply = buildString {
            append("I'm now following ${member.asMention}")
            if (followedMember != null) {
                append(' ').append("instead of ${followedMember.asMention}")
            }
            append('.')
        }
        event.reply(reply).queue()
    }
}
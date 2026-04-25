package com.github.mrbean355.roons.discord.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class LeaveCommand : BotCommand {

    override val name get() = "leave"
    override val description get() = "Leave the current voice channel."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return

        if (member.guild.audioManager.isConnected) {
            val channelName = member.guild.audioManager.connectedChannel?.name
            member.guild.audioManager.closeAudioConnection()
            event.reply("I'm disconnecting from `$channelName`.").queue()
        } else {
            event.reply("I'm not connected to a voice channel.").setEphemeral(true).queue()
        }
    }
}
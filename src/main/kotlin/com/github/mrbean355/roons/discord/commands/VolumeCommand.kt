package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

@Component
class VolumeCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val name get() = "volume"
    override val description get() = "Check the volume of sounds played in voice channels."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)

        event.reply("My volume is at `${settings.volume}%`.").setEphemeral(true).queue()
    }
}
package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.discord.audio.coerceVolume
import com.github.mrbean355.roons.service.DiscordBotService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

private const val NewVolumeOption = "new-volume"

@Component
class SetVolumeCommand(
    private val discordBotService: DiscordBotService
) : BotCommand {

    override val name get() = "set-volume"
    override val description get() = "Set the volume of sounds played in voice channels."

    override fun buildCommand(commandData: SlashCommandData) = commandData
        .addOption(OptionType.INTEGER, NewVolumeOption, "Volume percentage", true)

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val settings = discordBotService.loadSettings(member.guild.id)
        val newVolume = event.getOption(NewVolumeOption)?.asInt?.coerceVolume() ?: return

        discordBotService.saveSettings(settings.copy(volume = newVolume))
        event.reply("My volume has been set to `${newVolume}%`.").queue()
    }
}
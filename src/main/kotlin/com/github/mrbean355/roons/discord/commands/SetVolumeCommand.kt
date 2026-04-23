package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.discord.audio.coerceVolume
import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

private const val NewVolumeOption = "new-volume"

@Component
class SetVolumeCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BotCommand {

    override val name get() = "set-volume"
    override val description get() = "Set the volume of sounds played in voice channels."

    override fun buildCommand(commandData: SlashCommandData) = commandData
        .addOption(OptionType.INTEGER, NewVolumeOption, "Volume percentage", true)

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        val newVolume = event.getOption(NewVolumeOption)?.asInt?.coerceVolume() ?: return

        discordBotSettingsRepository.save(settings.copy(volume = newVolume))
        event.reply("My volume has been set to `${newVolume}%`.").queue()
    }
}
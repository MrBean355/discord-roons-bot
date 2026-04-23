package com.github.mrbean355.roons.discord.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

/** A slash command that users can type in a guild channel to interact with the bot. */
sealed interface BotCommand {

    /** Name used for slash commands. */
    val name: String

    /** Description used for slash commands. */
    val description: String

    /** Optionally build upon the [SlashCommandData] object. */
    fun buildCommand(commandData: SlashCommandData): SlashCommandData = commandData

    /** Handle the received slash command (e.g. /command). */
    fun handleCommand(event: SlashCommandInteractionEvent)

}
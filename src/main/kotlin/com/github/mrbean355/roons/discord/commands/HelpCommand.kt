/*
 * Copyright 2022 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons.discord.commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.springframework.stereotype.Component

private const val MESSAGE = "Interact with the bot using Discord's slash commands.\n" +
        "Just type slash (`/`) in a text channel and a list of available commands will pop up!\n" +
        "For more info or to log a bug, please visit: https://github.com/MrBean355/admiralbulldog-sounds/wiki/Discord-Bot"

@Component
class HelpCommand : BotCommand {

    override val name get() = "help"
    override val description get() = "Get some help with using the bot."

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        event.reply(MESSAGE).setEphemeral(true).queue()
    }
}
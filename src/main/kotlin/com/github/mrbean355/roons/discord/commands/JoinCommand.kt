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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class JoinCommand : BotCommand {

    override val name get() = "join"
    override val description get() = "Join a voice channel."

    override fun buildCommand(commandData: SlashCommandData) = commandData
        .addOption(OptionType.CHANNEL, "channel", "The channel to join. Optional; joins your current channel by default.")

    override fun handleCommand(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        val channelArg = event.getOption("channel")?.asChannel

        if (channelArg != null && channelArg !is AudioChannel) {
            event.reply("I can only connect to voice channels.").setEphemeral(true).queue()
            return
        }

        val channel = (channelArg?.asAudioChannel()) ?: member.voiceState?.channel
        if (channel == null) {
            event.reply("Please join a voice channel first.").setEphemeral(true).queue()
            return
        }

        if (member.guild.audioManager.isConnected) {
            val currentChannel = member.guild.audioManager.connectedChannel
            if (currentChannel?.idLong == channel.idLong) {
                event.reply("I'm already connected to `${currentChannel.name}`.").setEphemeral(true).queue()
                return
            }
        }

        val self = member.guild.selfMember
        when {
            !self.hasPermission(channel, Permission.VOICE_CONNECT) -> {
                event.reply("I don't have permission to connect to `${channel.name}`.").setEphemeral(true).queue()
            }

            !self.hasPermission(channel, Permission.VOICE_SPEAK) -> {
                event.reply("I don't have permission to speak in `${channel.name}`.").setEphemeral(true).queue()
            }

            else -> {
                member.guild.audioManager.openAudioConnection(channel)
                event.reply("I'm connecting to `${channel.name}`.").queue()
            }
        }
    }
}
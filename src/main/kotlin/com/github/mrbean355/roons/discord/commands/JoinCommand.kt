/*
 * Copyright 2021 Michael Johnston
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
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.springframework.stereotype.Component

@Component
class JoinCommand : BotCommand {

    override val name get() = "join"
    override val description get() = "Join your current voice channel."

    override fun process(event: SlashCommandEvent) {
        val guild = event.guild ?: return
        val channel = event.member?.voiceState?.channel
        if (channel == null) {
            event.queueEphemeralReply("You aren't in a voice channel.")
            return
        }
        if (guild.audioManager.isConnected) {
            val currentChannel = guild.audioManager.connectedChannel ?: return
            if (currentChannel.idLong == channel.idLong) {
                event.queueEphemeralReply("I'm already connected to `${currentChannel.name}`.")
                return
            }
        }
        val self = guild.selfMember
        if (!self.hasPermission(channel, Permission.VOICE_CONNECT)) {
            event.queueEphemeralReply("I don't have permission to connect to `${channel.name}`.")
        } else if (!self.hasPermission(channel, Permission.VOICE_SPEAK)) {
            event.queueEphemeralReply("I don't have permission to speak in `${channel.name}`.")
        } else {
            runCatching { guild.audioManager.openAudioConnection(channel) }
                .onSuccess { event.queueEphemeralReply("I've connected to `${channel.name}`!") }
                .onFailure { event.queueEphemeralReply("I can't connect to `${channel.name}` at the moment.") }
        }
    }
}
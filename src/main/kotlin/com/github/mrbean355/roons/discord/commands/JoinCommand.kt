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
import net.dv8tion.jda.api.entities.Member
import org.springframework.stereotype.Component

@Component
class JoinCommand : BasicCommand() {

    override val name get() = "join"
    override val description get() = "Join your current voice channel."

    override fun handleCommand(member: Member, reply: CommandReply) {
        val channel = member.voiceState?.channel
        if (channel == null) {
            reply("You aren't in a voice channel.")
            return
        }
        if (member.guild.audioManager.isConnected) {
            val currentChannel = member.guild.audioManager.connectedChannel
            if (currentChannel?.idLong == channel.idLong) {
                reply("I'm already connected to `${currentChannel.name}`.")
                return
            }
        }
        val self = member.guild.selfMember
        reply(
            if (!self.hasPermission(channel, Permission.VOICE_CONNECT)) {
                "I don't have permission to connect to `${channel.name}`."
            } else if (!self.hasPermission(channel, Permission.VOICE_SPEAK)) {
                "I don't have permission to speak in `${channel.name}`."
            } else {
                member.guild.audioManager.openAudioConnection(channel)
                "I've connected to `${channel.name}`!"
            }
        )
    }
}
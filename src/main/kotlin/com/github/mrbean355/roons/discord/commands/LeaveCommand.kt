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

import net.dv8tion.jda.api.entities.Member
import org.springframework.stereotype.Component

@Component
class LeaveCommand : BasicCommand() {

    override val legacyName get() = "seeya"
    override val name get() = "leave"
    override val description get() = "Leave the current voice channel."

    override fun handleCommand(member: Member, reply: CommandReply) {
        val audioManager = member.guild.audioManager
        if (member.guild.audioManager.isConnected) {
            val channelName = audioManager.connectedChannel?.name
            member.guild.audioManager.closeAudioConnection()
            reply("I've disconnected from `$channelName`.")
        } else {
            reply("I'm not connected to a voice channel.")
        }
    }
}
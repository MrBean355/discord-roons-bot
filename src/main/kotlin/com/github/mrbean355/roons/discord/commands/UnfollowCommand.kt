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

import com.github.mrbean355.roons.repository.DiscordBotSettingsRepository
import com.github.mrbean355.roons.repository.loadSettings
import net.dv8tion.jda.api.entities.Member
import org.springframework.stereotype.Component

@Component
class UnfollowCommand(
    private val discordBotSettingsRepository: DiscordBotSettingsRepository
) : BasicCommand() {

    override val name get() = "unfollow"
    override val description get() = "Stop following you when you join & leave voice channels."

    override fun handleCommand(member: Member, reply: CommandReply) {
        val settings = discordBotSettingsRepository.loadSettings(member.guild.id)
        val followedUser = settings.followedUser
        if (followedUser != null) {
            discordBotSettingsRepository.save(settings.copy(followedUser = null))
            val user = member.jda.getUserById(followedUser)?.asMention ?: "someone"
            reply("I've stopped following $user :ok_hand:")
        } else {
            reply("I'm not following anyone :shrug:")
        }
    }
}
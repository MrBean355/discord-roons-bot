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

package com.github.mrbean355.bulldog.api

import com.github.mrbean355.bulldog.api.dto.PlaySoundRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("discordController2")
@RequestMapping("/api/discord")
class DiscordController {

    /**
     * Find the Discord guild associated with the [token].
     */
    @GetMapping("/guilds/find")
    fun findGuildForToken(@RequestParam token: String): ResponseEntity<String> {
        throw UnsupportedOperationException()
    }

    /**
     * Play one or more sounds through a Discord voice channel.
     *
     * The guild will be determined from the [token], and the sounds will be played in order,
     * in the bot's current voice channel for the guild.
     */
    @PostMapping("/sounds/play")
    fun playSounds(
        @RequestParam token: String,
        @RequestBody sounds: List<PlaySoundRequest>,
    ): ResponseEntity<Void> {
        throw UnsupportedOperationException()
    }
}
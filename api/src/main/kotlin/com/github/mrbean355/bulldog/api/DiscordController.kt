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

import com.github.mrbean355.bulldog.api.dto.PlayMultipleSoundsRequest
import com.github.mrbean355.bulldog.api.dto.PlaySingleSoundRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class DiscordController {

    /**
     * Find the Discord guild associated with the [token].
     */
    @GetMapping("/lookupToken")
    fun lookupToken(@RequestParam("token") token: String): ResponseEntity<String> {
        throw UnsupportedOperationException()
    }

    /**
     * Play a sound through a Discord voice channel.
     *
     * The sound will be played in the bot's current voice channel for the guild.
     */
    @PostMapping("/")
    @Deprecated("Use playSounds() instead, as it also works for a single sound.")
    fun playSound(@RequestBody request: PlaySingleSoundRequest): ResponseEntity<Void> {
        throw UnsupportedOperationException()
    }

    /**
     * Play one or more sounds through a Discord voice channel.
     *
     * The sounds will be played in order, in the bot's current voice channel for the guild.
     */
    @PostMapping("/playSounds")
    fun playSounds(@RequestBody request: PlayMultipleSoundsRequest): ResponseEntity<Void> {
        throw UnsupportedOperationException()
    }
}
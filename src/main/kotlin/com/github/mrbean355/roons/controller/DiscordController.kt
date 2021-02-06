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

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.PlaySoundRequest
import com.github.mrbean355.roons.PlaySoundsRequest
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.discord.SoundStore
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.repository.updateLastSeen
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val DEFAULT_VOLUME = 100
private const val DEFAULT_RATE = 100

@RestController("/")
class DiscordController @Autowired constructor(
    private val discordBot: DiscordBot,
    private val appUserRepository: AppUserRepository,
    private val discordBotUserRepository: DiscordBotUserRepository,
    private val metadataRepository: MetadataRepository,
    private val soundStore: SoundStore
) {

    @GetMapping("lookupToken")
    fun lookupToken(@RequestParam("token") token: String): ResponseEntity<String> {
        val user = discordBotUserRepository.findOneByToken(token)
            ?: return ResponseEntity.notFound().build()

        val guild = discordBot.getGuildById(user.guildId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(guild.name)
    }

    @PostMapping
    fun playSound(@RequestBody request: PlaySoundRequest): ResponseEntity<Void> {
        appUserRepository.updateLastSeen(request.userId)

        if (request.token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val user = discordBotUserRepository.findOneByToken(request.token)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return if (soundStore.soundExists(request.soundFileName)) {
            if (discordBot.playSound(user, request.soundFileName, request.volume ?: DEFAULT_VOLUME, request.rate ?: DEFAULT_RATE)) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build()
            }
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PostMapping("playSounds")
    fun playSounds(@RequestBody request: PlaySoundsRequest): ResponseEntity<Void> {
        appUserRepository.updateLastSeen(request.userId)

        if (request.token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        if (request.sounds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val user = discordBotUserRepository.findOneByToken(request.token)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val results = request.sounds.map { sound ->
            discordBot.playSound(user, sound.soundFileName, sound.volume, sound.rate)
        }
        return if (results.any { it }) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @GetMapping("dumpStatus")
    fun dumpStatus(@RequestParam("token") token: String): ResponseEntity<String> {
        if (token.isBlank()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val adminToken = metadataRepository.adminToken
            ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        if (adminToken != token) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity.ok(discordBot.dumpStatus())
    }
}
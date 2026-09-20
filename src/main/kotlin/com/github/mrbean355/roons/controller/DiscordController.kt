package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.PlaySoundRequest
import com.github.mrbean355.roons.PlaySoundsRequest
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.service.DiscordBotService
import com.github.mrbean355.roons.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val DEFAULT_VOLUME = 100
private const val DEFAULT_RATE = 100

@RestController
@RequestMapping("/")
class DiscordController(
    private val discordBot: DiscordBot,
    private val userService: UserService,
    private val discordBotService: DiscordBotService
) {

    @GetMapping("lookupToken")
    fun lookupToken(@RequestParam("token") token: String): ResponseEntity<String> {
        val user = discordBotService.findUserByToken(token)
            ?: return ResponseEntity.notFound().build()

        val guild = discordBot.getGuildById(user.guildId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(guild.name)
    }

    @PostMapping
    fun playSound(@RequestBody request: PlaySoundRequest): ResponseEntity<Void> {
        userService.updateLastSeen(request.userId)

        if (request.token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val user = discordBotService.findUserByToken(request.token)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return if (discordBot.playSound(user, request.soundFileName, request.volume ?: DEFAULT_VOLUME, request.rate ?: DEFAULT_RATE)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PostMapping("playSounds")
    fun playSounds(@RequestBody request: PlaySoundsRequest): ResponseEntity<Void> {
        userService.updateLastSeen(request.userId)

        if (request.token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        if (request.sounds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val user = discordBotService.findUserByToken(request.token)
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
}
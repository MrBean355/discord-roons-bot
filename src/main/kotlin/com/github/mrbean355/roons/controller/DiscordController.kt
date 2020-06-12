package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.PlaySoundRequest
import com.github.mrbean355.roons.component.Statistics
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
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
        private val soundStore: SoundStore,
        private val statistics: Statistics
) {

    @RequestMapping("lookupToken", method = [GET])
    fun lookupToken(@RequestParam("token") token: String): ResponseEntity<String> {
        val user = discordBotUserRepository.findOneByToken(token)
                ?: return ResponseEntity.notFound().build()

        val guild = discordBot.getGuildById(user.guildId)
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(guild.name)
    }

    @RequestMapping(method = [POST])
    fun playSound(@RequestBody request: PlaySoundRequest): ResponseEntity<Void> {
        appUserRepository.updateLastSeen(request.userId)

        if (request.token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val user = discordBotUserRepository.findOneByToken(request.token)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return if (soundStore.soundExists(request.soundFileName)) {
            if (discordBot.playSound(user, request.soundFileName, request.volume ?: DEFAULT_VOLUME, request.rate
                            ?: DEFAULT_RATE)) {
                statistics.increment(Statistics.Type.DISCORD_SOUNDS)
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build()
            }
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @RequestMapping("dumpStatus", method = [GET])
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
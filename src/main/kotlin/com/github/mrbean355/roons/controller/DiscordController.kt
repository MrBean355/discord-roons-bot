package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.PlaySoundRequest
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.discord.SoundStore
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@RestController("/")
class DiscordController @Autowired constructor(
        private val discordBot: DiscordBot,
        private val appUserRepository: AppUserRepository,
        private val discordBotUserRepository: DiscordBotUserRepository,
        private val metadataRepository: MetadataRepository,
        private val soundStore: SoundStore
) {

    @RequestMapping(method = [POST])
    fun playSound(@RequestBody request: PlaySoundRequest): ResponseEntity<Void> {
        val appUser = appUserRepository.findByGeneratedId(request.userId) ?: AppUser(0, request.userId, null)
        appUserRepository.save(appUser.copy(lastSeen = Date()))

        if (request.token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val user = discordBotUserRepository.findOneByToken(request.token)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return if (soundStore.soundExists(request.soundFileName)) {
            if (discordBot.playSound(user.token, request.soundFileName)) {
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
        val adminToken = metadataRepository.findByKey("admin_token")
                ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        if (adminToken.value != token) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity.ok(discordBot.dumpStatus())
    }
}
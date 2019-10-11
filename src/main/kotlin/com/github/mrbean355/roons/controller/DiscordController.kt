package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.PlaySoundRequest
import com.github.mrbean355.roons.component.Analytics
import com.github.mrbean355.roons.discord.RunesDiscordBot
import com.github.mrbean355.roons.discord.SoundStore
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.DiscordBotUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@RestController("/")
class DiscordController @Autowired constructor(private val runesDiscordBot: RunesDiscordBot, private val appUserRepository: AppUserRepository, private val discordBotUserRepository: DiscordBotUserRepository,
                                               private val soundStore: SoundStore, private val analytics: Analytics) {

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
            runesDiscordBot.playSound(user.token, request.soundFileName)
            analytics.logEvent(request.userId, "sound_played_discord", request.soundFileName)
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }
}
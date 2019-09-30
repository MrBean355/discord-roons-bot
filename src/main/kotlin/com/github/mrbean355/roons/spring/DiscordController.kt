package com.github.mrbean355.roons.spring

import com.github.mrbean355.roons.discord.RunesDiscordBot
import com.github.mrbean355.roons.discord.SoundStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class DiscordController @Autowired constructor(private val runesDiscordBot: RunesDiscordBot, private val userRepository: UserRepository) {

    @RequestMapping
    fun playSound(@Param("token") token: String?, @Param("soundFileName") soundFileName: String?): ResponseEntity<Void> {
        if (token.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val user = userRepository.findOneByToken(token)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return if (SoundStore.soundExists(soundFileName.orEmpty())) {
            runesDiscordBot.playSound(user.token, soundFileName.orEmpty())
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }
}
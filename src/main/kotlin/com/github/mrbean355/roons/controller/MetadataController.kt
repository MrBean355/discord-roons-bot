package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metadata")
class MetadataController @Autowired constructor(
    private val metadataRepository: MetadataRepository,
    private val context: ApplicationContext,
    private val discordBot: DiscordBot
) {

    @RequestMapping("shutdown", method = [GET])
    fun shutdown(@RequestParam("token") token: String): ResponseEntity<String> {
        if (token.isBlank()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val adminToken = metadataRepository.adminToken
            ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        if (adminToken != token) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        discordBot.shutdown()
        (context as? ConfigurableApplicationContext)?.close()
        return ResponseEntity.ok("Goodbye")
    }
}
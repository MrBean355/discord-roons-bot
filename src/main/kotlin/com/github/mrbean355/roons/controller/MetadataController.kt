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

    @RequestMapping("laterVersion", method = [GET])
    fun hasLaterVersion(@RequestParam("version") version: String): ResponseEntity<Boolean> {
        // As of app version 1.8.0, we no longer call this service to check for a new app release.
        // Instead, we use the GitHub API to determine what the latest release is.
        // Therefore, only old apps will call this service. There will always be a new version for them.
        return ResponseEntity.ok(true)
    }

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
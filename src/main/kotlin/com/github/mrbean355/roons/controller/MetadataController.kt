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

import com.github.mrbean355.roons.WelcomeMessageResponse
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.repository.getWelcomeMessage
import com.github.mrbean355.roons.repository.saveWelcomeMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/metadata")
class MetadataController @Autowired constructor(
        private val metadataRepository: MetadataRepository,
        private val context: ApplicationContext,
        private val discordBot: DiscordBot,
        private val cacheManager: CacheManager
) {

    @GetMapping("welcomeMessage")
    @WelcomeMessageCache
    fun getWelcomeMessage(): ResponseEntity<WelcomeMessageResponse> {
        val message = metadataRepository.getWelcomeMessage()
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(WelcomeMessageResponse(message))
    }

    @PutMapping("welcomeMessage")
    fun putWelcomeMessage(@RequestParam("token") token: String, @RequestParam("message") message: String): ResponseEntity<Void> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }

        metadataRepository.saveWelcomeMessage(message)
        cacheManager.getCache(WELCOME_MESSAGE_CACHE_NAME)?.clear()

        return ResponseEntity.ok().build()
    }

    @GetMapping("shutdown")
    fun shutdown(@RequestParam("token") token: String): ResponseEntity<String> {
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

private const val WELCOME_MESSAGE_CACHE_NAME = "welcome_message_cache"

@Cacheable(WELCOME_MESSAGE_CACHE_NAME)
private annotation class WelcomeMessageCache
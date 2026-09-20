package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.WelcomeMessageResponse
import com.github.mrbean355.roons.security.AdminOnly
import com.github.mrbean355.roons.service.MetadataService
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metadata")
class MetadataController(
    private val metadataService: MetadataService,
    private val cacheManager: CacheManager
) {

    @GetMapping("welcomeMessage")
    @WelcomeMessageCache
    fun getWelcomeMessage(): ResponseEntity<WelcomeMessageResponse> {
        val message = metadataService.getWelcomeMessage()
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(WelcomeMessageResponse(message))
    }

    @AdminOnly
    @PutMapping("welcomeMessage")
    fun putWelcomeMessage(
        @RequestParam("message") message: String
    ): ResponseEntity<Void> {
        metadataService.saveWelcomeMessage(message)
        cacheManager.getCache(WELCOME_MESSAGE_CACHE_NAME)?.clear()

        return ResponseEntity.ok().build()
    }
}

private const val WELCOME_MESSAGE_CACHE_NAME = "welcome_message_cache"

@Cacheable(WELCOME_MESSAGE_CACHE_NAME)
private annotation class WelcomeMessageCache
package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.Metadata
import com.github.mrbean355.roons.repository.MetadataRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetadataService(
    private val metadataRepository: MetadataRepository
) {

    private val adminToken: String?
        get() = metadataRepository.findByKey(KEY_ADMIN_TOKEN)?.value

    @WelcomeMessageCache
    @Transactional(readOnly = true)
    fun getWelcomeMessage(): String? {
        return metadataRepository.findByKey(KEY_WELCOME_MESSAGE)?.value
    }

    @ClearWelcomeMessageCache
    @Transactional
    fun saveWelcomeMessage(message: String) {
        val metadata = metadataRepository.findByKey(KEY_WELCOME_MESSAGE)?.copy(value = message)
            ?: Metadata(KEY_WELCOME_MESSAGE, message)
        metadataRepository.save(metadata)
    }

    @Transactional(readOnly = true)
    fun isValidAdminToken(authHeader: String?): Boolean {
        val configuredToken = adminToken ?: return false
        val headerToken = if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
            authHeader.substring(7).trim()
        } else {
            authHeader?.trim()
        }
        return !headerToken.isNullOrEmpty() && headerToken == configuredToken
    }

    @Transactional(readOnly = true)
    fun hasAdminToken(): Boolean {
        return adminToken != null
    }
}

private const val KEY_ADMIN_TOKEN = "admin_token"
private const val KEY_WELCOME_MESSAGE = "app_welcome_message"


private const val WELCOME_MESSAGE_CACHE_NAME = "welcome_message_cache"

@Cacheable(WELCOME_MESSAGE_CACHE_NAME)
private annotation class WelcomeMessageCache

@CacheEvict(WELCOME_MESSAGE_CACHE_NAME, allEntries = true)
private annotation class ClearWelcomeMessageCache


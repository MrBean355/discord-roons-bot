package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.repository.getWelcomeMessage
import com.github.mrbean355.roons.repository.isValidAdminToken
import com.github.mrbean355.roons.repository.saveWelcomeMessage
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetadataService(
    private val metadataRepository: MetadataRepository
) {

    @WelcomeMessageCache
    @Transactional(readOnly = true)
    fun getWelcomeMessage(): String? {
        return metadataRepository.getWelcomeMessage()
    }

    @ClearWelcomeMessageCache
    @Transactional
    fun saveWelcomeMessage(message: String) {
        metadataRepository.saveWelcomeMessage(message)
    }

    @Transactional(readOnly = true)
    fun isValidAdminToken(authHeader: String?): Boolean {
        return metadataRepository.isValidAdminToken(authHeader)
    }

    @Transactional(readOnly = true)
    fun hasAdminToken(): Boolean {
        return metadataRepository.adminToken != null
    }
}

private const val WELCOME_MESSAGE_CACHE_NAME = "welcome_message_cache"

@Cacheable(WELCOME_MESSAGE_CACHE_NAME)
private annotation class WelcomeMessageCache

@CacheEvict(WELCOME_MESSAGE_CACHE_NAME, allEntries = true)
private annotation class ClearWelcomeMessageCache


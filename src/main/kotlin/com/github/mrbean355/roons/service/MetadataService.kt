package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.repository.getWelcomeMessage
import com.github.mrbean355.roons.repository.isValidAdminToken
import com.github.mrbean355.roons.repository.saveWelcomeMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetadataService(
    private val metadataRepository: MetadataRepository
) {

    @Transactional(readOnly = true)
    fun getWelcomeMessage(): String? {
        return metadataRepository.getWelcomeMessage()
    }

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

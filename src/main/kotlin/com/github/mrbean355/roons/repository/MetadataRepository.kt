package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.Metadata
import org.springframework.data.repository.CrudRepository

interface MetadataRepository : CrudRepository<Metadata, String> {
    fun findByKey(key: String): Metadata?
}

private const val KEY_ADMIN_TOKEN = "admin_token"
private const val KEY_WELCOME_MESSAGE = "app_welcome_message"

val MetadataRepository.adminToken: String?
    get() = findByKey(KEY_ADMIN_TOKEN)?.value

fun MetadataRepository.getWelcomeMessage(): String? =
    findByKey(KEY_WELCOME_MESSAGE)?.value

fun MetadataRepository.saveWelcomeMessage(newMessage: String) {
    val metadata = findByKey(KEY_WELCOME_MESSAGE)?.copy(value = newMessage)
        ?: Metadata(KEY_WELCOME_MESSAGE, newMessage)

    save(metadata)
}

fun MetadataRepository.isValidAdminToken(authHeader: String?, tokenParam: String?): Boolean {
    val configuredToken = adminToken ?: return false
    val headerToken = if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
        authHeader.substring(7).trim()
    } else {
        authHeader?.trim()
    }
    return (!headerToken.isNullOrEmpty() && headerToken == configuredToken) ||
            (!tokenParam.isNullOrEmpty() && tokenParam == configuredToken)
}


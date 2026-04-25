package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.stereotype.Component

@Component
class Analytics(
    private val appUserRepository: AppUserRepository,
    private val analyticsPropertyRepository: AnalyticsPropertyRepository
) {

    fun logProperties(userId: String, properties: Map<String, String>): Boolean {
        val user = appUserRepository.findByGeneratedId(userId)
        if (user == null || properties.isEmpty()) {
            return false
        }

        val existing = analyticsPropertyRepository.findByUserAndPropertyIn(user, properties.keys.toList())
        val entities = properties.map { (property, value) ->
            existing.firstOrNull { it.property == property }?.copy(value = value)
                ?: AnalyticsProperty(0, user, property, value)
        }

        analyticsPropertyRepository.saveAll(entities)
        return true
    }

    fun logCommandUsage(discordUserId: String, commandName: String) {
        val user = appUserRepository.findByGeneratedId(discordUserId)
            ?: appUserRepository.save(AppUser(0, discordUserId, null))

        analyticsPropertyRepository.save(AnalyticsProperty(0, user, "command_usage", commandName))
    }
}
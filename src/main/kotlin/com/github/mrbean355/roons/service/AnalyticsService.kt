package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnalyticsService(
    private val appUserRepository: AppUserRepository,
    private val analyticsPropertyRepository: AnalyticsPropertyRepository
) {

    @Transactional
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

    @Transactional
    fun logCommandUsage(discordUserId: String, commandName: String) {
        val user = appUserRepository.findByGeneratedId(discordUserId)
            ?: appUserRepository.save(AppUser(0, discordUserId, null))

        analyticsPropertyRepository.save(AnalyticsProperty(0, user, "command_usage", commandName))
    }

    @Transactional(readOnly = true)
    fun findDistinctProperties(): List<String> {
        return analyticsPropertyRepository.findDistinctProperties()
    }

    @Transactional(readOnly = true)
    fun getStatistic(property: String): Map<String, Int>? {
        val properties = analyticsPropertyRepository.findByProperty(property)
        if (properties.isEmpty()) {
            return null
        }
        return properties.flatMap { it.value.split(',') }
            .groupingBy { it }
            .eachCount()
    }
}

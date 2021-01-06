package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Analytics @Autowired constructor(
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
}
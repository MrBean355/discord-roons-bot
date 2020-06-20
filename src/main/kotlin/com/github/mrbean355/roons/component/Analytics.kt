package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.AnalyticsEvent
import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.repository.AnalyticsEventRepository
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Date

@Component
class Analytics @Autowired constructor(
        private val analyticsEventRepository: AnalyticsEventRepository,
        private val appUserRepository: AppUserRepository,
        private val analyticsPropertyRepository: AnalyticsPropertyRepository
) {

    fun logEvent(userId: String, eventType: String, eventData: String): Boolean {
        val nonEmptyUserId = userId.ifEmpty { "unknown" }
        val userEvent = analyticsEventRepository.findByAppUserIdAndEventTypeAndEventData(nonEmptyUserId, eventType, eventData)
                ?: AnalyticsEvent(0, nonEmptyUserId, eventType, eventData, 0, null)

        analyticsEventRepository.save(userEvent.copy(count = userEvent.count.inc(), lastOccurred = Date()))
        return true
    }

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
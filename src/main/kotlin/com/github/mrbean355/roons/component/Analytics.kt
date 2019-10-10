package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.AnalyticsEvent
import com.github.mrbean355.roons.repository.AnalyticsEventRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Date

@Component
class Analytics @Autowired constructor(private val analyticsEventRepository: AnalyticsEventRepository) {

    fun logEvent(userId: String, eventType: String, eventData: String): Boolean {
        val nonEmptyUserId = userId.ifEmpty { "unknown" }
        val userEvent = analyticsEventRepository.findByAppUserIdAndEventTypeAndEventData(nonEmptyUserId, eventType, eventData)
                ?: AnalyticsEvent(0, nonEmptyUserId, eventType, eventData, 0, null)

        analyticsEventRepository.save(userEvent.copy(count = userEvent.count.inc(), lastOccurred = Date()))
        return true
    }
}
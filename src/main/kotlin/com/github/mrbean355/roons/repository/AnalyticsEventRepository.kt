package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.AnalyticsEvent
import org.springframework.data.repository.CrudRepository

interface AnalyticsEventRepository : CrudRepository<AnalyticsEvent, Int> {
    fun findByAppUserIdAndEventTypeAndEventData(userId: String, eventType: String, eventData: String): AnalyticsEvent?
}

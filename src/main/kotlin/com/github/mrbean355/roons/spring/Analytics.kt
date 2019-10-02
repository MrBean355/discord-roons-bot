package com.github.mrbean355.roons.spring

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class Analytics @Autowired constructor(private val userEventRepository: UserEventRepository) {

    fun logEvent(userId: String, eventType: String, eventData: String): Boolean {
        if (!isValidId(userId)) {
            return false
        }
        val userEvent = userEventRepository.findByUserIdAndEventTypeAndEventData(userId, eventType, eventData)
                ?: UserEvent(0, userId, eventType, eventData, 0)

        userEventRepository.save(userEvent.copy(count = userEvent.count.inc()))
        return true
    }

    private fun isValidId(id: String): Boolean {
        return try {
            UUID.fromString(id)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
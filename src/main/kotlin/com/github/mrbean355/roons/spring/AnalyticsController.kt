package com.github.mrbean355.roons.spring

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/analytics")
class AnalyticsController @Autowired constructor(private val userEventRepository: UserEventRepository) {

    @RequestMapping("logEvent", method = [POST])
    fun logEvent(@RequestBody request: AnalyticsRequest): ResponseEntity<AnalyticsResponse> {
        var userId = request.userId
        if (userId == null) {
            // Client doesn't have an ID yet, give them one!
            userId = generateId()
        } else if (!isValidId(userId)) {
            // Generate a new, valid ID and return it, but don't save anything yet.
            // Hopefully they will save it and send it in future.
            return ResponseEntity.ok(AnalyticsResponse(generateId()))
        }
        // Try find an existing event so we can increment the count.
        val userEvent = userEventRepository.findByUserIdAndEventTypeAndEventData(userId, request.eventType, request.eventData)
                ?: UserEvent(0, userId, request.eventType, request.eventData, 0)

        userEventRepository.save(userEvent.copy(count = userEvent.count.inc()))
        return ResponseEntity.ok(AnalyticsResponse(userId))
    }

    private fun isValidId(id: String): Boolean {
        return try {
            UUID.fromString(id)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun generateId() = UUID.randomUUID().toString()
}

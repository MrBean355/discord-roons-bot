package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AnalyticsRequest
import com.github.mrbean355.roons.component.Analytics
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/analytics")
class AnalyticsController @Autowired constructor(private val appUserRepository: AppUserRepository, private val analytics: Analytics) {

    @RequestMapping("logEvent", method = [POST])
    fun logEvent(@RequestBody request: AnalyticsRequest): ResponseEntity<Void> {
        appUserRepository.updateLastSeen(request.userId)

        return if (analytics.logEvent(request.userId, request.eventType, request.eventData)) {
            ResponseEntity.ok()
        } else {
            ResponseEntity.badRequest()
        }.build()
    }
}

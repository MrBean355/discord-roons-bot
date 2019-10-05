package com.github.mrbean355.roons.spring

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@RestController
@RequestMapping("/analytics")
class AnalyticsController @Autowired constructor(private val appUserRepository: AppUserRepository, private val analytics: Analytics) {

    @RequestMapping("logEvent", method = [POST])
    fun logEvent(@RequestBody request: AnalyticsRequest): ResponseEntity<Void> {
        val appUser = appUserRepository.findByGeneratedId(request.userId)
        if (appUser != null) {
            appUserRepository.save(appUser.copy(lastSeen = Date()))
        }
        return if (analytics.logEvent(request.userId, request.eventType, request.eventData)) {
            ResponseEntity.ok()
        } else {
            ResponseEntity.badRequest()
        }.build()
    }
}

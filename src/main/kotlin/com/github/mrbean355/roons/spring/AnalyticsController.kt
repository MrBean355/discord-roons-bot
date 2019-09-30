package com.github.mrbean355.roons.spring

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/analytics")
class AnalyticsController @Autowired constructor(private val userEventRepository: UserEventRepository) {

    @RequestMapping("logEvent", method = [POST])
    fun logEvent(@RequestBody request: AnalyticsRequest) {
        userEventRepository.save(UserEvent(0, request.userId, request.eventType, request.eventData))
    }
}

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AnalyticsRequest
import com.github.mrbean355.roons.component.Analytics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/analytics")
class AnalyticsController @Autowired constructor(
    private val analytics: Analytics
) {

    @RequestMapping("logProperties", method = [POST])
    fun logProperties(@RequestBody request: AnalyticsRequest): ResponseEntity<Void> {
        return if (analytics.logProperties(request.userId, request.properties)) {
            ResponseEntity.ok()
        } else {
            ResponseEntity.badRequest()
        }.build()
    }
}

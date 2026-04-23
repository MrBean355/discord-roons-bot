package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AnalyticsRequest
import com.github.mrbean355.roons.component.Analytics
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/analytics")
class AnalyticsController(
    private val analytics: Analytics
) {

    @PostMapping("logProperties")
    fun logProperties(@RequestBody request: AnalyticsRequest): ResponseEntity<Void> {
        return if (analytics.logProperties(request.userId, request.properties)) {
            ResponseEntity.ok()
        } else {
            ResponseEntity.badRequest()
        }.build()
    }
}

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.StatisticsResponse
import com.github.mrbean355.roons.getTimeAgo
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MINUTE

@RestController
@RequestMapping("/statistics")
class StatisticsController(
        private val appUserRepository: AppUserRepository,
        private val analyticsPropertyRepository: AnalyticsPropertyRepository,
        private val metadataRepository: MetadataRepository
) {

    @GetMapping("/get")
    fun getStatistics(@RequestParam("token") token: String): ResponseEntity<StatisticsResponse> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity.ok(StatisticsResponse(
                recentUsers = appUserRepository.findByLastSeenAfter(getTimeAgo(5, MINUTE)).size,
                dailyUsers = appUserRepository.findByLastSeenAfter(getTimeAgo(1, DAY_OF_MONTH)).size,
                properties = constructProperties(listOf(
                        "app.version",
                        "app.distribution",
                        "app.update",
                        "sounds.update",
                        "tray.enabled",
                        "tray.permanent",
                        "bot.enabled",
                        "mod.enabled",
                        "mod.version",
                        "mod.update"
                ))
        ))
    }

    private fun constructProperties(stats: List<String>): Map<String, Map<String, Int>> {
        return stats.associateWith(this::countValues)
    }

    private fun countValues(property: String): Map<String, Int> {
        return analyticsPropertyRepository.findByProperty(property).groupingBy { it.value }.eachCount()
    }
}
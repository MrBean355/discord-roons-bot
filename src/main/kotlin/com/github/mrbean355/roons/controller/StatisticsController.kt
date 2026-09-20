package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DiscordServerDto
import com.github.mrbean355.roons.SystemHealthResponse
import com.github.mrbean355.roons.component.Clock
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.security.AdminOnly
import com.github.mrbean355.roons.service.AnalyticsService
import com.github.mrbean355.roons.service.SystemHealthService
import com.github.mrbean355.roons.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.concurrent.TimeUnit

@AdminOnly
@RestController
@RequestMapping("/statistics")
class StatisticsController(
    private val userService: UserService,
    private val analyticsService: AnalyticsService,
    private val systemHealthService: SystemHealthService,
    private val discordBot: DiscordBot,
    private val clock: Clock
) {

    @GetMapping("health")
    fun getHealth(): ResponseEntity<SystemHealthResponse> {
        return ResponseEntity.ok(systemHealthService.getSystemHealth())
    }

    @GetMapping("properties")
    fun listProperties(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(analyticsService.findDistinctProperties())
    }

    @GetMapping("recentUsers")
    fun getRecentUsers(
        @RequestParam("period") period: Long
    ): ResponseEntity<Long> {
        val since = clock.currentTimeMs - TimeUnit.MINUTES.toMillis(period)
        return ResponseEntity.ok(userService.countRecentUsers(Instant.ofEpochMilli(since)))
    }

    @GetMapping("{property}")
    fun getStatistic(
        @PathVariable("property") property: String
    ): ResponseEntity<Map<String, Int>> {
        val statistics = analyticsService.getStatistic(property)
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        return ResponseEntity.ok(statistics)
    }

    @GetMapping("discordServers")
    fun getDiscordServers(): ResponseEntity<List<DiscordServerDto>> {
        return ResponseEntity.ok(
            discordBot.getGuilds().map {
                DiscordServerDto(
                    it.name,
                    it.memberCount,
                    it.audioManager.connectedChannel?.name
                )
            }
        )
    }
}
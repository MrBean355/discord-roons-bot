package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DiscordServerDto
import com.github.mrbean355.roons.SystemHealthResponse
import com.github.mrbean355.roons.component.Clock
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.security.AdminOnly
import com.github.mrbean355.roons.service.AnalyticsService
import com.github.mrbean355.roons.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.management.ManagementFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@AdminOnly
@RestController
@RequestMapping("/statistics")
class StatisticsController(
    private val userService: UserService,
    private val analyticsService: AnalyticsService,
    private val discordBot: DiscordBot,
    private val clock: Clock
) {

    @GetMapping("health")
    fun getHealth(): ResponseEntity<SystemHealthResponse> {
        val runtime = ManagementFactory.getRuntimeMXBean()
        val mem = Runtime.getRuntime()
        val uptime = Duration.ofMillis(runtime.uptime)

        return ResponseEntity.ok(
            SystemHealthResponse(
                uptime = formatDuration(uptime),
                memoryUsage = "${(mem.totalMemory() - mem.freeMemory()) / 1024 / 1024} MB / ${mem.maxMemory() / 1024 / 1024} MB",
                discordStatus = discordBot.getGatewayStatus().name,
                discordPing = discordBot.getGatewayPing()
            )
        )
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        return "${days}d ${hours}h ${minutes}m"
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
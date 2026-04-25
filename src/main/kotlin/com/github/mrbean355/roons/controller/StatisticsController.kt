package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DiscordServerDto
import com.github.mrbean355.roons.SystemHealthResponse
import com.github.mrbean355.roons.component.Clock
import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
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

@RestController
@RequestMapping("/statistics")
class StatisticsController(
    private val appUserRepository: AppUserRepository,
    private val analyticsPropertyRepository: AnalyticsPropertyRepository,
    private val metadataRepository: MetadataRepository,
    private val discordBot: DiscordBot,
    private val clock: Clock
) {

    @GetMapping("health")
    fun getHealth(@RequestParam("token") token: String): ResponseEntity<SystemHealthResponse> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
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
    fun listProperties(@RequestParam("token") token: String): ResponseEntity<List<String>> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity.ok(analyticsPropertyRepository.findDistinctProperties())
    }

    @GetMapping("recentUsers")
    fun getRecentUsers(@RequestParam("token") token: String, @RequestParam("period") period: Long): ResponseEntity<Long> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        val since = clock.currentTimeMs - TimeUnit.MINUTES.toMillis(period)
        return ResponseEntity.ok(appUserRepository.countByLastSeenAfter(Instant.ofEpochMilli(since)))
    }

    @GetMapping("{property}")
    fun getStatistic(@RequestParam("token") token: String, @PathVariable("property") property: String): ResponseEntity<Map<String, Int>> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        val properties = analyticsPropertyRepository.findByProperty(property)
        if (properties.isEmpty()) {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
        return ResponseEntity.ok(
            properties.flatMap { it.value.split(',') }
                .groupingBy { it }
                .eachCount()
        )
    }

    @GetMapping("discordServers")
    fun getDiscordServers(@RequestParam("token") token: String): ResponseEntity<List<DiscordServerDto>> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
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
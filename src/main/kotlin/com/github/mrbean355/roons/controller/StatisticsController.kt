/*
 * Copyright 2021 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DiscordServerDto
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
import java.util.Date
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
        return ResponseEntity.ok(appUserRepository.countByLastSeenAfter(Date(since)))
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
                    it.region.getName(),
                    it.audioManager.connectedChannel?.name
                )
            }
        )
    }
}
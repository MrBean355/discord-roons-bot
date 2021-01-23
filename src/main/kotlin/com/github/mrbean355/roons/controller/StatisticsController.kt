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
                        "mod.update",
                ) + soundTriggerTypes()) + ("mod.selection" to modSelection())
        ))
    }

    private fun constructProperties(stats: List<String>): Map<String, Map<String, Int>> {
        return stats.associateWith(this::countValues)
    }

    private fun countValues(property: String): Map<String, Int> {
        return analyticsPropertyRepository.findByProperty(property).groupingBy { it.value }.eachCount()
    }

    private fun soundTriggerTypes(): Collection<String> {
        return listOf("onBountyRunesSpawn", "onDeath", "onDefeat", "onHeal", "onKill", "onMatchStart", "onMidasReady",
                "onRespawn", "onSmoked", "onVictory", "periodically")
                .map { "sounds.triggers.$it" }
    }

    private fun modSelection(): Map<String, Int> {
        return analyticsPropertyRepository.findByProperty("mod.selection")
                .flatMap { it.value.split(',') }
                .groupingBy { it }
                .eachCount()
                .mapKeys {
                    if (it.key.isBlank()) "(none)" else it.key
                }
    }
}
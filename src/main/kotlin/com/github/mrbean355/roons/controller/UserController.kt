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

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.CreateIdResponse
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import java.util.UUID

@RestController
@RequestMapping("/", method = [POST])
class UserController @Autowired constructor(
    private val appUserRepository: AppUserRepository,
    private val analyticsPropertyRepository: AnalyticsPropertyRepository
) {
    @RequestMapping("createId")
    fun createId(): ResponseEntity<CreateIdResponse> {
        var tries = 0
        var generated: String
        do {
            generated = UUID.randomUUID().toString()
        } while (++tries < 10 && appUserRepository.countByGeneratedId(generated) > 0)
        if (tries >= 10) {
            return ResponseEntity.status(HttpStatus.LOOP_DETECTED).build()
        }
        appUserRepository.save(AppUser(0, generated, Date()))
        return ResponseEntity.ok(CreateIdResponse(generated))
    }

    @RequestMapping("heartbeat")
    fun heartbeat(@RequestParam("userId") userId: String) {
        appUserRepository.updateLastSeen(userId)
    }

    @GetMapping("findPeers")
    fun findPeers(@RequestParam("userId") userId: String): ResponseEntity<List<String>> {
        val user = appUserRepository.findByGeneratedId(userId)
            ?: return ResponseEntity.notFound().build()

        val matchId = analyticsPropertyRepository.findByUserAndProperty(user, "dota.matchId")?.value
            ?: return ResponseEntity.notFound().build()

        if (matchId.isBlank()) {
            return ResponseEntity.ok(emptyList())
        }

        val users = analyticsPropertyRepository.findByPropertyAndValue("dota.matchId", matchId)
            .map { it.user }

        return ResponseEntity.ok(users.mapNotNull {
            analyticsPropertyRepository.findByUserAndProperty(it, "dota.heroName")?.value
        })
    }
}
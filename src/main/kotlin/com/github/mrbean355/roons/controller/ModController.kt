/*
 * Copyright 2022 Michael Johnston
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

import com.github.mrbean355.roons.DotaModDto
import com.github.mrbean355.roons.asDto
import com.github.mrbean355.roons.orNull
import com.github.mrbean355.roons.repository.DotaModRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.telegram.TelegramNotifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mods")
class ModController(
    private val dotaModRepository: DotaModRepository,
    private val metadataRepository: MetadataRepository,
    private val telegramNotifier: TelegramNotifier,
    private val cacheManager: CacheManager
) {

    @GetMapping
    @DotaModCache
    fun listMods(): List<DotaModDto> = dotaModRepository.findAll().map { it.asDto() }

    @GetMapping("{key}")
    @DotaModCache
    fun getMod(@PathVariable("key") key: String): ResponseEntity<DotaModDto> {
        val mod = dotaModRepository.findById(key).orNull()
            ?: return ResponseEntity(NOT_FOUND)

        return ResponseEntity.ok(mod.asDto())
    }

    @PatchMapping("{key}")
    fun patchMod(
        @PathVariable("key") key: String,
        @RequestParam("hash") hash: String,
        @RequestParam("size") size: Int,
        @RequestParam("token") token: String,
        @RequestParam("message") message: String?
    ): ResponseEntity<Void> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(UNAUTHORIZED)
        }
        val mod = dotaModRepository.findById(key).orNull()
            ?: return ResponseEntity(NOT_FOUND)

        dotaModRepository.save(mod.copy(size = size, hash = hash))
        cacheManager.getCache(DOTA_MOD_CACHE_NAME)?.clear()

        if (message != null) {
            telegramNotifier.sendChannelMessage(message)
        }

        return ResponseEntity.ok().build()
    }

    @GetMapping("refresh")
    fun refreshMods(@RequestParam("token") token: String): ResponseEntity<Void> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(UNAUTHORIZED)
        }
        cacheManager.getCache(DOTA_MOD_CACHE_NAME)?.clear()
        return ResponseEntity.ok().build()
    }
}

private const val DOTA_MOD_CACHE_NAME = "dota_mod_cache"

@Cacheable(DOTA_MOD_CACHE_NAME)
private annotation class DotaModCache
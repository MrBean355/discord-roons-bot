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

package com.github.mrbean355.bulldog.api

import com.github.mrbean355.bulldog.api.dto.DotaModDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mods")
class DotaModController {

    /**
     * Get a list of available Dota mods.
     */
    @GetMapping("/")
    fun list(): ResponseEntity<List<DotaModDto>> {
        throw UnsupportedOperationException()
    }

    /**
     * Lookup the Dota mod with the given [key].
     */
    @GetMapping("/{key}")
    fun get(@PathVariable("key") key: String): ResponseEntity<DotaModDto> {
        throw UnsupportedOperationException()
    }

    /**
     * Update the Dota mod with the given [key].
     */
    @PatchMapping("/{key}")
    fun update(
        @PathVariable("key") key: String,
        @RequestParam("hash") hash: String,
        @RequestParam("size") size: Int,
        @RequestParam("token") token: String,
        @RequestParam("message") message: String?
    ): ResponseEntity<Void> {
        throw UnsupportedOperationException()
    }

    // TODO: Still needed?
    @GetMapping("/refresh")
    fun refresh(@RequestParam("token") token: String): ResponseEntity<Void> {
        throw UnsupportedOperationException()
    }
}
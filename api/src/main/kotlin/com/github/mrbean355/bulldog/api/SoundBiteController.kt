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

import com.github.mrbean355.bulldog.api.dto.SoundBiteDto
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/soundBites")
class SoundBiteController {

    /**
     * Get a list of available sound bites.
     */
    @GetMapping("/listV2")
    @Deprecated("Use listV3() instead, as it supports sound bite categories.")
    fun listV2(): Map<String, String> {
        throw UnsupportedOperationException()
    }

    /**
     * Get a list of available sound bites.
     */
    @GetMapping("/v3/list")
    fun listV3(): Collection<SoundBiteDto> {
        throw UnsupportedOperationException()
    }

    /**
     * Download a single sound bite.
     *
     * @param name Name of the sound bite, e.g. "roons.mp3".
     */
    @GetMapping("/{name}")
    fun get(@PathVariable("name") name: String): ResponseEntity<Resource> {
        throw UnsupportedOperationException()
    }
}
/*
 * Copyright 2023 Michael Johnston
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

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/statistics")
class StatisticsController {

    @GetMapping("/properties")
    fun listProperties(@RequestParam("token") token: String): ResponseEntity<List<String>> {
        throw UnsupportedOperationException()
    }

    @GetMapping("/recentUsers")
    fun getRecentUsers(@RequestParam("token") token: String, @RequestParam("period") period: Long): ResponseEntity<Long> {
        throw UnsupportedOperationException()
    }

    @GetMapping("/{property}")
    fun getStatistic(@RequestParam("token") token: String, @PathVariable("property") property: String): ResponseEntity<Map<String, Int>> {
        throw UnsupportedOperationException()
    }
}
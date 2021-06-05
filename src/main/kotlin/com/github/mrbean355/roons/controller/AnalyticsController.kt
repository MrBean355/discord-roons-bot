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

import com.github.mrbean355.roons.AnalyticsRequest
import com.github.mrbean355.roons.component.Analytics
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/analytics")
class AnalyticsController(
    private val analytics: Analytics
) {

    @PostMapping("logProperties")
    fun logProperties(@RequestBody request: AnalyticsRequest): ResponseEntity<Void> {
        return if (analytics.logProperties(request.userId, request.properties)) {
            ResponseEntity.ok()
        } else {
            ResponseEntity.badRequest()
        }.build()
    }
}

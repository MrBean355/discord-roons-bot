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

import com.github.mrbean355.bulldog.api.dto.FeedbackRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController("userController2")
@RequestMapping("/api/users")
class UserController {

    /**
     * Create a new ID for a user.
     */
    @PostMapping
    fun create(): ResponseEntity<String> {
        throw UnsupportedOperationException()
    }

    /**
     * Send a heartbeat, indicating that the user was recently active.
     *
     * @param id User ID, as returned by [create].
     */
    @PostMapping("/{id}/heartbeat")
    fun heartbeat(@PathVariable id: String): ResponseEntity<Void> {
        throw UnsupportedOperationException()
    }

    /**
     * Send some feedback about the application.
     *
     * @param id User ID.
     * @param request Feedback data.
     */
    @PostMapping("/{id}/feedback")
    fun feedback(
        @PathVariable id: String,
        @RequestBody request: FeedbackRequest,
    ): ResponseEntity<Void> {
        throw UnsupportedOperationException()
    }
}
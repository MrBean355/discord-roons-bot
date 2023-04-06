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

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.FeedbackRequest
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.telegram.TelegramNotifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/feedback")
class FeedbackController(
    private val appUserRepository: AppUserRepository,
    private val telegramNotifier: TelegramNotifier
) {

    @PostMapping
    fun postFeedback(@RequestBody request: FeedbackRequest): ResponseEntity<Void> {
        appUserRepository.findByGeneratedId(request.userId)
            ?: return ResponseEntity.notFound().build()

        telegramNotifier.sendPrivateMessage(
            """
            📋 <b>Feedback received</b>
            Rating: ${request.rating}
            Comments: ${request.comments}
            """.trimIndent()
        )

        return ResponseEntity.ok().build()
    }
}
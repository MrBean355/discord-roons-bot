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
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class FeedbackControllerTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var telegramNotifier: TelegramNotifier
    private lateinit var feedbackController: FeedbackController

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        feedbackController = FeedbackController(appUserRepository, telegramNotifier)
    }

    @Test
    internal fun testPostFeedback_UserNotFound_ReturnsNotFoundResponse() {
        every { appUserRepository.findByGeneratedId("12345") } returns null

        val result = feedbackController.postFeedback(FeedbackRequest("12345", 0, ""))

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testPostFeedback_UserFound_SendsTelegramMessage() {
        every { appUserRepository.findByGeneratedId("12345") } returns mockk()
        justRun { telegramNotifier.sendPrivateMessage(any()) }

        feedbackController.postFeedback(FeedbackRequest("12345", 5, "Best app ever POGGIES!"))

        verify {
            telegramNotifier.sendPrivateMessage(
                """
                📋 <b>Feedback received</b>
                Rating: 5
                Comments: Best app ever POGGIES!
                """.trimIndent()
            )
        }
    }

    @Test
    internal fun testPostFeedback_UserFound_ReturnsOkResponse() {
        every { appUserRepository.findByGeneratedId("12345") } returns mockk()
        justRun { telegramNotifier.sendPrivateMessage(any()) }

        val result = feedbackController.postFeedback(FeedbackRequest("12345", 0, ""))

        assertSame(HttpStatus.OK, result.statusCode)
    }
}
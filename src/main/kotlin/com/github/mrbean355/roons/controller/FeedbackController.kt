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
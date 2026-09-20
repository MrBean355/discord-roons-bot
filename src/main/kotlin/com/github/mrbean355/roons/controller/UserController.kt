package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.CreateIdResponse
import com.github.mrbean355.roons.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class UserController(
    private val userService: UserService
) {
    @PostMapping("createId")
    fun createId(): ResponseEntity<CreateIdResponse> {
        val generated = userService.createId()
            ?: return ResponseEntity.status(HttpStatus.LOOP_DETECTED).build()

        return ResponseEntity.ok(CreateIdResponse(generated))
    }

    @PostMapping("heartbeat")
    fun heartbeat(@RequestParam("userId") userId: String) {
        userService.updateLastSeen(userId)
    }
}
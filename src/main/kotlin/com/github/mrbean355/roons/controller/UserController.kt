package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.CreateIdResponse
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import java.util.UUID

@RestController
@RequestMapping("/")
class UserController(
    private val appUserRepository: AppUserRepository
) {
    @PostMapping("createId")
    fun createId(): ResponseEntity<CreateIdResponse> {
        var tries = 0
        var generated: String
        do {
            generated = UUID.randomUUID().toString()
        } while (++tries < 10 && appUserRepository.countByGeneratedId(generated) > 0)
        if (tries >= 10) {
            return ResponseEntity.status(HttpStatus.LOOP_DETECTED).build()
        }
        appUserRepository.save(AppUser(0, generated, Date()))
        return ResponseEntity.ok(CreateIdResponse(generated))
    }

    @PostMapping("heartbeat")
    fun heartbeat(@RequestParam("userId") userId: String) {
        appUserRepository.updateLastSeen(userId)
    }
}
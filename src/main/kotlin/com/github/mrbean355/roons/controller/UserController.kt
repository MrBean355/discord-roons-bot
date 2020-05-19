package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.CreateIdResponse
import com.github.mrbean355.roons.component.Statistics
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import java.util.UUID

@RestController
@RequestMapping("/", method = [POST])
class UserController @Autowired constructor(
        private val appUserRepository: AppUserRepository,
        private val statistics: Statistics
) {
    @RequestMapping("createId")
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
        statistics.increment(Statistics.Type.NEW_USERS)
        return ResponseEntity.ok(CreateIdResponse(generated))
    }

    @RequestMapping("heartbeat")
    fun heartbeat(@RequestParam("userId") userId: String) {
        appUserRepository.updateLastSeen(userId)
    }
}
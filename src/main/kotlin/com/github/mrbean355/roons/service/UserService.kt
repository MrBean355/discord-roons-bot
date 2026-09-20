package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.component.Clock
import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val appUserRepository: AppUserRepository,
    private val clock: Clock
) {

    @Transactional
    fun createId(): String? {
        var tries = 0
        var generated: String
        do {
            generated = UUID.randomUUID().toString()
        } while (++tries < 10 && appUserRepository.countByGeneratedId(generated) > 0)
        if (tries >= 10) {
            return null
        }
        appUserRepository.save(AppUser(0, generated, clock.now))
        return generated
    }

    @Transactional
    fun updateLastSeen(userId: String) {
        require(userId.isNotBlank())
        val user = appUserRepository.findByGeneratedId(userId) ?: AppUser(0, userId, null)
        appUserRepository.save(user.copy(lastSeen = clock.now))
    }

    @Transactional(readOnly = true)
    fun exists(userId: String): Boolean {
        return appUserRepository.findByGeneratedId(userId) != null
    }

    @Transactional(readOnly = true)
    fun countRecentUsers(since: Instant): Long {
        return appUserRepository.countByLastSeenAfter(since)
    }

    @Transactional(readOnly = true)
    fun findByGeneratedId(userId: String): AppUser? {
        return appUserRepository.findByGeneratedId(userId)
    }

    @Transactional
    fun getOrCreateUser(userId: String): AppUser {
        return appUserRepository.findByGeneratedId(userId)
            ?: appUserRepository.save(AppUser(0, userId, null))
    }
}

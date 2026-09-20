package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.updateLastSeen
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val appUserRepository: AppUserRepository
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
        appUserRepository.save(AppUser(0, generated, Instant.now()))
        return generated
    }

    @Transactional
    fun updateLastSeen(userId: String) {
        appUserRepository.updateLastSeen(userId)
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

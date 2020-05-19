package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.AppUser
import org.springframework.data.repository.CrudRepository
import java.util.Date

interface AppUserRepository : CrudRepository<AppUser, Int> {
    fun countByGeneratedId(generatedId: String): Int
    fun findByGeneratedId(generatedId: String): AppUser?
    fun findByLastSeenAfter(date: Date): List<AppUser>
}

/** Update the user's last seen time to now. */
fun AppUserRepository.updateLastSeen(userId: String) {
    require(userId.isNotBlank())
    val user = findByGeneratedId(userId) ?: AppUser(0, userId, null)
    save(user.copy(lastSeen = Date()))
}

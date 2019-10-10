package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.AppUser
import org.springframework.data.repository.CrudRepository

interface AppUserRepository : CrudRepository<AppUser, Int> {
    fun countByGeneratedId(generatedId: String): Int
    fun findByGeneratedId(generatedId: String): AppUser?
}


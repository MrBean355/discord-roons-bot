package com.github.mrbean355.roons.spring

import org.springframework.data.repository.CrudRepository

interface AppUserRepository : CrudRepository<AppUser, Int> {
    fun countByGeneratedId(generatedId: String): Int
}


package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.Metadata
import org.springframework.data.repository.CrudRepository

interface MetadataRepository : CrudRepository<Metadata, String> {
    fun findByKey(key: String): Metadata?
}



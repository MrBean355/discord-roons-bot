package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.Metadata
import org.springframework.data.repository.CrudRepository

interface MetadataRepository : CrudRepository<Metadata, String> {
    fun findByKey(key: String): Metadata?
}

val MetadataRepository.adminToken: String?
    get() = findByKey("admin_token")?.value

fun MetadataRepository.takeStartupMessage(): String? {
    val metadata = findByKey("startup_message") ?: return null
    delete(metadata)
    return metadata.value
}

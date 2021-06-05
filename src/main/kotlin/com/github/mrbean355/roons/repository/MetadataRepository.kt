/*
 * Copyright 2021 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.Metadata
import org.springframework.data.repository.CrudRepository

interface MetadataRepository : CrudRepository<Metadata, String> {
    fun findByKey(key: String): Metadata?
}

private const val KEY_ADMIN_TOKEN = "admin_token"
private const val KEY_STARTUP_MESSAGE = "startup_message"
private const val KEY_WELCOME_MESSAGE = "app_welcome_message"
private const val KEY_UPDATE_SLASH_COMMANDS = "update_slash_commands"

val MetadataRepository.adminToken: String?
    get() = findByKey(KEY_ADMIN_TOKEN)?.value

fun MetadataRepository.takeStartupMessage(): String? {
    val metadata = findByKey(KEY_STARTUP_MESSAGE) ?: return null
    delete(metadata)
    return metadata.value
}

fun MetadataRepository.getWelcomeMessage(): String? =
    findByKey(KEY_WELCOME_MESSAGE)?.value

fun MetadataRepository.saveWelcomeMessage(newMessage: String) {
    val metadata = findByKey(KEY_WELCOME_MESSAGE)?.copy(value = newMessage)
        ?: Metadata(KEY_WELCOME_MESSAGE, newMessage)

    save(metadata)
}

fun MetadataRepository.takeUpdateSlashCommandsFlag(): Boolean {
    val metadata = findByKey(KEY_UPDATE_SLASH_COMMANDS) ?: return false
    delete(metadata)
    return true
}
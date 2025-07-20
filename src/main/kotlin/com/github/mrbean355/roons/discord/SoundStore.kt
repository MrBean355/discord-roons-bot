/*
 * Copyright 2023 Michael Johnston
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

package com.github.mrbean355.roons.discord

import jakarta.annotation.PostConstruct
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.springframework.stereotype.Component
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

private const val SOUNDS_DIR = "sounds"

@Component
class SoundStore(
    private val logger: Logger,
) {

    private var soundsCache: Map<String, String> = emptyMap()

    @PostConstruct
    fun unpackSounds() {
        val manifest = readSoundResource("manifest.json").decodeToString()
            .let { Json.decodeFromString<List<String>>(it) }

        val output = File(SOUNDS_DIR).apply {
            deleteRecursively()
            mkdirs()
        }

        manifest.forEach { name ->
            File(output, name).also {
                it.writeBytes(readSoundResource(name))
                soundsCache += name to it.checksum()
            }
        }

        val copied = output.listFiles().map { it.name }
        check(manifest.all { it in copied }) {
            "Failed to copy all sounds."
        }

        logger.info("Total sounds: ${manifest.size}")
    }

    /** @return collection of all available sound bites. */
    fun listAll(): Map<String, String> {
        return soundsCache
    }

    /** @return [File] for the specified [soundFileName] if it exists, `null` otherwise. */
    fun getFile(soundFileName: String): File? {
        return File(SOUNDS_DIR, soundFileName.trim()).let {
            if (it.exists()) it else null
        }
    }

    private fun readSoundResource(name: String): ByteArray {
        val stream = SoundStore::class.java.classLoader.getResourceAsStream("sounds/$name")
        require(stream != null) { "Couldn't find sound resource: $name" }

        return stream.use {
            it.readBytes()
        }
    }

    private fun File.checksum(): String {
        val messageDigest = MessageDigest.getInstance("SHA-512")
        val result = messageDigest.digest(readBytes())
        val convertedResult = BigInteger(1, result)
        var hashText = convertedResult.toString(16)
        while (hashText.length < 32) {
            hashText = "0$hashText"
        }
        return hashText
    }
}
/*
 * Copyright 2022 Michael Johnston
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

import com.github.mrbean355.roons.PlaySound
import com.github.mrbean355.roons.component.PlaySounds
import com.github.mrbean355.roons.telegram.TelegramNotifier
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private const val SOUNDS_DIR = "sounds"
private const val TEMP_SOUNDS_DIR = "sounds_temp"
private const val LOCAL_CACHE_FILE = "cache.json"

@Component
class SoundStore(
    private val playSounds: PlaySounds,
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger
) {
    private val lock = ReentrantReadWriteLock()
    private var soundsCache: Map<String, PlaySound> = emptyMap()

    @Value("\${roons.soundBites.localCache:false}")
    private var useLocalCache: Boolean = false

    @PostConstruct
    fun onPostConstruct(): Unit = runBlocking(IO) {
        if (useLocalCache && File(LOCAL_CACHE_FILE).exists()) {
            loadSoundBitesFromDisk()
        } else {
            downloadAllSoundBites()
        }
    }

    /** @return collection of all available sound bites. */
    fun listAll(): Collection<PlaySound> = lock.read {
        soundsCache.values
    }

    /** @return [File] for the specified [soundFileName] if it exists, `null` otherwise. */
    fun getFile(soundFileName: String): File? = lock.read {
        File(SOUNDS_DIR, soundFileName.trim()).let {
            if (it.exists()) it else null
        }
    }

    /**
     * Synchronise our collection of sound bites with the PlaySounds page.
     * Re-downloads all sounds to make sure that sounds with the same name but different content are also downloaded.
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun synchroniseSoundBites(): Unit = runBlocking(IO) {
        val old = soundsCache
        downloadAllSoundBites()

        sendTelegramNotification(
            addedFiles = soundsCache.keys - old.keys,
            changedFiles = soundsCache.filterKeys { it in old }.filter { it.value.checksum != old.getValue(it.key).checksum }.keys,
            removedFiles = old.keys - soundsCache.keys
        )
    }

    private fun loadSoundBitesFromDisk() {
        soundsCache = Gson().fromJson(
            File(LOCAL_CACHE_FILE).readText(),
            object : TypeToken<Map<String, PlaySound>>() {}.type
        )
        logger.info("Loaded ${soundsCache.size} sounds from disk cache.")
    }

    private suspend fun downloadAllSoundBites() {
        val tempSoundsDir = File(TEMP_SOUNDS_DIR).also {
            if (it.exists()) {
                it.deleteRecursively()
            }
            it.mkdir()
        }

        val remoteFiles = tryListRemoteFiles()
        val mapping = remoteFiles.associateBy { it.name }

        coroutineScope {
            remoteFiles.forEach { file ->
                launch {
                    tryDownloadFile(file)
                }
            }
        }

        lock.write {
            val soundsDir = File(SOUNDS_DIR)
            soundsDir.deleteRecursively()
            tempSoundsDir.renameTo(soundsDir)

            soundsCache = soundsDir.listFiles()
                .orEmpty()
                .associateWith { PlaySound(it.name, it.checksum(), mapping.getValue(it.nameWithoutExtension).category) }
                .mapKeys { it.key.name }

            if (useLocalCache) {
                File(LOCAL_CACHE_FILE).apply {
                    writeText(Gson().toJson(soundsCache))
                    logger.info("Wrote ${soundsCache.size} sounds to disk cache (${length() / 1024f} KB).")
                }
            }
        }
    }

    private fun tryListRemoteFiles(): List<PlaySounds.RemoteSoundFile> {
        return try {
            playSounds.listRemoteFiles()
        } catch (t: Throwable) {
            logger.error("Error listing sound bites", t)
            telegramNotifier.sendPrivateMessage("⚠️ Error listing sound bites: ${t.message}")
            throw t
        }
    }

    private fun tryDownloadFile(file: PlaySounds.RemoteSoundFile) {
        try {
            playSounds.downloadFile(file, TEMP_SOUNDS_DIR)
        } catch (t: Throwable) {
            logger.error("Error downloading $file", t)
            telegramNotifier.sendPrivateMessage("⚠️ Error downloading ${file.name} (${file.url}): ${t.message}")
            throw t
        }
    }

    private fun sendTelegramNotification(addedFiles: Collection<String>, changedFiles: Collection<String>, removedFiles: Collection<String>) {
        val message = buildString {
            if (addedFiles.isNotEmpty()) {
                append("<b>Added:</b> ")
                appendLine(addedFiles.removeExtensions().joinToString())
            }
            if (changedFiles.isNotEmpty()) {
                append("<b>Changed:</b> ")
                appendLine(changedFiles.removeExtensions().joinToString())
            }
            if (removedFiles.isNotEmpty()) {
                append("<b>Removed:</b> ")
                appendLine(removedFiles.removeExtensions().joinToString())
            }
        }
        if (message.isNotEmpty()) {
            telegramNotifier.sendChannelMessage("🔊 Play Sounds Updated 🔊\n\n$message")
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

    private fun Iterable<String>.removeExtensions(): List<String> =
        map { it.substringBeforeLast('.') }
}
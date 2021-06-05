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

package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.component.PlaySounds
import com.github.mrbean355.roons.telegram.TelegramNotifier
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
import javax.annotation.PostConstruct
import kotlin.concurrent.read
import kotlin.concurrent.write

private const val SOUNDS_DIR = "sounds"
private const val TEMP_SOUNDS_DIR = "sounds_temp"

@Component
class SoundStore(
    private val playSounds: PlaySounds,
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger
) {
    private val lock = ReentrantReadWriteLock()
    private var fileChecksums: Map<String, String> = emptyMap()

    @Value("\${roons.soundBites.skipFirstDownload:false}")
    private var skipFirstDownload: Boolean = false

    @PostConstruct
    fun onPostConstruct(): Unit = runBlocking(IO) {
        if (skipFirstDownload) {
            loadSoundBitesFromDisk()
        } else {
            downloadAllSoundBites()
        }
    }

    /** @return map of sound bite name to its file's checksum. */
    fun listAll(): Map<String, String> = lock.read {
        fileChecksums
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
    @Scheduled(cron = "0 0 * * * *")
    fun synchroniseSoundBites(): Unit = runBlocking(IO) {
        val old = fileChecksums
        downloadAllSoundBites()

        sendTelegramNotification(
            addedFiles = fileChecksums.keys - old.keys,
            changedFiles = fileChecksums.filter { it.key in old.keys }.filter { it.value != old[it.key] }.keys,
            removedFiles = old.keys - fileChecksums.keys
        )
    }

    private fun loadSoundBitesFromDisk() {
        fileChecksums = File(SOUNDS_DIR).listFiles().orEmpty().associate {
            it.name to it.checksum()
        }
    }

    private suspend fun downloadAllSoundBites() {
        val tempSoundsDir = File(TEMP_SOUNDS_DIR).also {
            if (it.exists()) {
                it.deleteRecursively()
            }
            it.mkdir()
        }

        try {
            coroutineScope {
                playSounds.listRemoteFiles().forEach { file ->
                    launch {
                        playSounds.downloadFile(file, TEMP_SOUNDS_DIR)
                    }
                }
            }
        } catch (t: Throwable) {
            logger.error("Error downloading sound bites", t)
            telegramNotifier.sendPrivateMessage(t.message.orEmpty())
            throw t
        }

        lock.write {
            val soundsDir = File(SOUNDS_DIR)
            soundsDir.deleteRecursively()
            tempSoundsDir.renameTo(soundsDir)
            fileChecksums = soundsDir.listFiles()
                .orEmpty()
                .associateWith { it.checksum() }
                .mapKeys { it.key.name }
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
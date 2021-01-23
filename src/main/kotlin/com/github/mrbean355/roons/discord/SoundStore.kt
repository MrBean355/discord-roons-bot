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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import javax.annotation.PostConstruct

@Component
class SoundStore @Autowired constructor(
    private val playSounds: PlaySounds,
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger
) {
    private val coroutineScope = CoroutineScope(IO + SupervisorJob())
    private var soundsDirectory = SoundsDirectory.PRIMARY
    private var fileChecksums: Map<String, String> = emptyMap()

    @Value("\${roons.soundBites.skipFirstDownload:false}")
    private var skipFirstDownload: Boolean = false

    @PostConstruct
    fun onPostConstruct() {
        if (skipFirstDownload) {
            fileChecksums = File(soundsDirectory.dirName).listFiles().orEmpty().associate {
                it.name to it.checksum()
            }
            if (fileChecksums.isNotEmpty()) {
                return
            }
        }
        runBlocking {
            coroutineScope.launch {
                fileChecksums = downloadAllSoundBites(soundsDirectory)
            }.join() // wait for sound bites to download.
        }
    }

    /** @return map of sound bite name to its file's checksum. */
    fun listAll(): Map<String, String> {
        return fileChecksums
    }

    /** @return [File] for the specified [soundFileName] if it exists, `null` otherwise. */
    fun getFile(soundFileName: String): File? {
        return soundsDirectory.getSound(soundFileName.trim())
    }

    /** @return `true` if the sound file exists, `false` otherwise. */
    fun soundExists(soundFileName: String): Boolean {
        return getFile(soundFileName) != null
    }

    /**
     * Synchronise our collection of sound bites with the PlaySounds page.
     * Re-downloads all sounds to make sure that sounds with the same name but different content are also downloaded.
     * Downloads into the non-active [SoundsDirectory] and then flips the active & non-active directories.
     */
    @Scheduled(initialDelayString = "PT1H", fixedRateString = "PT1H")
    fun synchroniseSoundBites() {
        coroutineScope.launch {
            val old = fileChecksums
            val nextDirectory = soundsDirectory.other()
            val new = downloadAllSoundBites(nextDirectory)
            fileChecksums = new
            soundsDirectory = nextDirectory

            val addedSounds = new.keys - old.keys
            val removedSounds = old.keys - new.keys

            if (addedSounds.size > 15 || removedSounds.size > 15) {
                // Occasionally, 2 strange Telegram messages are sent.
                // The first one says that many new sounds were added (but they have existed for a long time).
                // The second one says that many sounds were removed (even though they still exist).
                // Instead of sending the message, send a private debugging one to figure out why it happens.

                telegramNotifier.sendPrivateMessage(
                    """
                    Something weird happened.
                    Added: ${addedSounds.size}
                    Removed: ${removedSounds.size}
                    Before sync: ${old.size}
                    """.trimIndent()
                )
            } else {
                sendTelegramNotification(
                    addedSounds = addedSounds,
                    changedSounds = new.filter { it.key in old.keys }.filter { it.value != old.getValue(it.key) }.keys,
                    removedSounds = removedSounds
                )
            }
        }
    }

    private suspend fun downloadAllSoundBites(soundsDirectory: SoundsDirectory): Map<String, String> {
        val destination = File(soundsDirectory.dirName)
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()

        coroutineScope {
            listRemoteSoundBites().forEach { remoteSoundFile ->
                launch {
                    if (!downloadSoundBite(remoteSoundFile, soundsDirectory)) {
                        copyFallbackFile(remoteSoundFile, soundsDirectory)
                    }
                }
            }
        }

        val files = destination.listFiles()
        if (files == null || files.isEmpty()) {
            telegramNotifier.sendPrivateMessage("Failed to list destination files")
            throw IllegalStateException("Failed to list files for ${destination.name}")
        }

        return files.associateWith { it.checksum() }
            .mapKeys { it.key.name }
    }

    /**
     * Fetch a list of the sounds that exist on the PlaySounds page.
     * Retries the operation if it fails, a max of 5 times.
     */
    private fun listRemoteSoundBites(attempts: Int = 5): List<PlaySounds.RemoteSoundFile> {
        return try {
            playSounds.listRemoteFiles()
        } catch (t: Throwable) {
            if (attempts > 0) {
                listRemoteSoundBites(attempts - 1)
            } else {
                logger.error("Failed to list remote sounds", t)
                telegramNotifier.sendPrivateMessage("Couldn't reach the PlaySounds page after 5 tries")
                throw t
            }
        }
    }

    /**
     * Download a [remoteSoundFile] and place it in the [soundsDirectory].
     * Retries the download if it fails, a max of 5 times.
     *
     * @return `true` if the file was downloaded, `false` otherwise.
     */
    private fun downloadSoundBite(remoteSoundFile: PlaySounds.RemoteSoundFile, soundsDirectory: SoundsDirectory, attempts: Int = 5): Boolean {
        return try {
            playSounds.downloadFile(remoteSoundFile, soundsDirectory.dirName)
            true
        } catch (t: Throwable) {
            if (attempts > 0) {
                downloadSoundBite(remoteSoundFile, soundsDirectory, attempts - 1)
            } else {
                logger.error("Failed to download $remoteSoundFile", t)
                telegramNotifier.sendPrivateMessage("Couldn't download ${remoteSoundFile.name} after 5 tries")
                false
            }
        }
    }

    /**
     * Try to copy the local file from the alternate directory to the current one.
     *
     * @return `true` if the file was copied, `false` if no such file exists.
     */
    private fun copyFallbackFile(remoteSoundFile: PlaySounds.RemoteSoundFile, target: SoundsDirectory) {
        val source = target.other()
        val fallback = source.getSound(remoteSoundFile.fileName)
        if (fallback == null) {
            logger.error("No fallback for ${remoteSoundFile.name} in ${source.dirName}, giving up")
        } else {
            fallback.copyTo(File(target.dirName, remoteSoundFile.fileName))
        }
    }

    private fun sendTelegramNotification(addedSounds: Collection<String>, changedSounds: Collection<String>, removedSounds: Collection<String>) {
        val message = buildString {
            if (addedSounds.isNotEmpty()) {
                append("<b>Added:</b> ")
                appendLine(addedSounds.joinToString())
            }
            if (changedSounds.isNotEmpty()) {
                append("<b>Changed:</b> ")
                appendLine(changedSounds.joinToString())
            }
            if (removedSounds.isNotEmpty()) {
                append("<b>Removed:</b> ")
                appendLine(removedSounds.joinToString())
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

    private enum class SoundsDirectory(val dirName: String) {
        PRIMARY("sounds"),
        SECONDARY("sounds_alt");

        fun getSound(name: String): File? {
            val file = File(dirName, name)
            return if (file.exists()) file else null
        }

        fun other(): SoundsDirectory {
            return if (this == PRIMARY) SECONDARY else PRIMARY
        }
    }
}
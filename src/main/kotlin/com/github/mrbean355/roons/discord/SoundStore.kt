package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.component.PlaySounds
import com.github.mrbean355.roons.component.Statistics
import com.github.mrbean355.roons.telegram.TelegramNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import javax.annotation.PostConstruct

/** Folder within resources where special sounds live. */
private const val SPECIAL_SOUNDS_PATH = "special_sounds"

/** Special sounds that don't exist on the PlaySounds page. */
private val SPECIAL_SOUNDS = listOf("useyourmidas.mp3", "wefuckinglost.mp3")

@Component
class SoundStore @Autowired constructor(
        private val playSounds: PlaySounds,
        private val telegramNotifier: TelegramNotifier,
        private val logger: Logger,
        private val statistics: Statistics
) {
    private val coroutineScope = CoroutineScope(IO + SupervisorJob())
    private var soundsDirectory = SoundsDirectory.PRIMARY
    private var fileChecksums: Map<String, String> = emptyMap()

    @PostConstruct
    fun onPostConstruct() {
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
            sendTelegramNotification(
                    newFiles = new.keys - old.keys,
                    changedFiles = new.filter { it.key in old.keys }.filter { it.value != old.getValue(it.key) }.keys,
                    oldFiles = old.keys - new.keys
            )
        }
    }

    @Scheduled(fixedRateString = "P1D")
    fun sendStatisticsNotification() {
        if (statistics.isEmpty()) {
            return
        }
        telegramNotifier.sendMessage("""
                📈 <b>Stats from the last day</b>:
                Discord sounds: ${statistics.take(Statistics.Type.DISCORD_SOUNDS)}
                Discord commands: ${statistics.take(Statistics.Type.DISCORD_COMMANDS)}
                New app users: ${statistics.take(Statistics.Type.NEW_USERS)}
            """.trimIndent())
    }

    private suspend fun downloadAllSoundBites(soundsDirectory: SoundsDirectory): Map<String, String> {
        val destination = File(soundsDirectory.dirName)
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()

        coroutineScope {
            playSounds.listRemoteFiles().forEach { remoteSoundFile ->
                launch {
                    if (!downloadSoundBite(remoteSoundFile, soundsDirectory)) {
                        if (!copyFallbackFile(remoteSoundFile, soundsDirectory)) {
                            telegramNotifier.sendMessage("⚠️ Failed to download ${remoteSoundFile.fileName}")
                        }
                    }
                }
            }
        }

        copySpecialSounds(soundsDirectory)

        return destination.listFiles()?.toList().orEmpty()
                .associateWith { it.checksum() }
                .mapKeys { it.key.name }
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
            logger.error("Failed to download $remoteSoundFile", t)
            if (attempts > 0) {
                logger.info("Retrying $remoteSoundFile")
                downloadSoundBite(remoteSoundFile, soundsDirectory, attempts - 1)
            } else {
                false
            }
        }
    }

    /**
     * Try to copy the local file from the alternate directory to the current one.
     *
     * @return `true` if the file was copied, `false` if no such file exists.
     */
    private fun copyFallbackFile(remoteSoundFile: PlaySounds.RemoteSoundFile, target: SoundsDirectory): Boolean {
        val source = target.other()
        val fallback = source.getSound(remoteSoundFile.localFileName)
        return if (fallback == null) {
            logger.error("No fallback for ${remoteSoundFile.fileName} in ${source.dirName}, giving up")
            false
        } else {
            fallback.copyTo(File(target.dirName, remoteSoundFile.localFileName))
            true
        }
    }

    private fun sendTelegramNotification(newFiles: Collection<String>, changedFiles: Collection<String>, oldFiles: Collection<String>) {
        val message = buildString {
            if (newFiles.isNotEmpty()) {
                appendln("Added sounds:")
                newFiles.forEach {
                    appendln("- ${it.substringBeforeLast('.')}")
                }
            }
            if (changedFiles.isNotEmpty()) {
                if (isNotEmpty()) {
                    appendln()
                }
                appendln("Changed sounds:")
                changedFiles.forEach {
                    appendln("- ${it.substringBeforeLast('.')}")
                }
            }
            if (oldFiles.isNotEmpty()) {
                if (isNotEmpty()) {
                    appendln()
                }
                appendln("Removed sounds:")
                oldFiles.forEach {
                    appendln("- ${it.substringBeforeLast('.')}")
                }
            }
        }
        if (message.isNotEmpty()) {
            telegramNotifier.sendChannelMessage("🔊 Play Sounds Updated 🔊\n\n$message")
        }
    }

    /** Copy special sounds from resources to the destination folder. */
    private fun copySpecialSounds(soundsDirectory: SoundsDirectory) {
        SPECIAL_SOUNDS.forEach { sound ->
            if (soundsDirectory.getSound(sound) == null) {
                SoundStore::class.java.classLoader.getResourceAsStream("$SPECIAL_SOUNDS_PATH/$sound")?.use { input ->
                    FileOutputStream(File(soundsDirectory.dirName, sound)).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                logger.warn("Sound already exists; not copying special sound: $sound")
            }
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
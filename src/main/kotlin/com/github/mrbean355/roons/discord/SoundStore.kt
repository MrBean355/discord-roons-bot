package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.component.PlaySounds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue

/** Folder to store downloaded sounds in. */
private const val SOUNDS_PATH = "sounds"

/** Folder within resources where special sounds live. */
private const val SPECIAL_SOUNDS_PATH = "special_sounds"

/** Special sounds that don't exist on the PlaySounds page. */
private val SPECIAL_SOUNDS = listOf("herewegoagain.mp3", "useyourmidas.mp3", "wefuckinglost.mp3")

@Component
class SoundStore @Autowired constructor(private val playSounds: PlaySounds, private val logger: Logger) {
    private val fileChecksums = mutableMapOf<String, String>()
    private val mutex = Mutex()

    /** @return names of all downloaded sounds. */
    fun listAll(): Map<String, String> {
        waitForSynchronising()
        return File(SOUNDS_PATH).listFiles()?.toList().orEmpty().associateWith {
            fileChecksums.getOrPut(it.name) { it.checksum() }
        }.mapKeys { it.key.name }
    }

    /** @return [File] for the specified [soundFileName] if it exists, `null` otherwise. */
    fun getFile(soundFileName: String): File? {
        waitForSynchronising()
        return getFileInternal(soundFileName)
    }

    /** @return `true` if the sound file exists, `false` otherwise. */
    fun soundExists(soundFileName: String): Boolean {
        waitForSynchronising()
        return soundExistsInternal(soundFileName)
    }

    /**
     * Sync our local sounds with the PlaySounds page.
     * Downloads sounds which don't exist locally.
     * Deletes local sounds which don't exist remotely.
     * Scheduled for once per hour.
     */
    @Scheduled(fixedRate = 3_600_000)
    fun synchroniseSounds() = GlobalScope.launch {
        mutex.withLock {
            val localFiles = ConcurrentLinkedQueue(getLocalFiles())
            val remoteFiles = playSounds.listRemoteFiles()
            coroutineScope {
                remoteFiles.forEach { remoteFile ->
                    launch {
                        val existsLocally = localFiles.remove(remoteFile.localFileName)
                        if (!existsLocally) {
                            playSounds.downloadFile(remoteFile, SOUNDS_PATH)
                            logger.info("Downloaded: ${remoteFile.localFileName}")
                        }
                    }
                }
            }
            localFiles.forEach {
                File("$SOUNDS_PATH/$it").delete()
                logger.info("Deleted old sound: $it")
            }
            copySpecialSounds()
            logger.info("Done synchronising")
        }
    }

    /**
     * Wait for the [synchroniseSounds] method to finish downloading all sounds (max 10 seconds).
     * We don't want to allow the app to check what sounds exist if we're still downloading them.
     * Otherwise an incomplete list of sounds will be returned.
     */
    private fun waitForSynchronising() {
        var attempts = 0
        while (mutex.isLocked) {
            Thread.sleep(100)
            ++attempts
            if (attempts >= 100) {
                throw IllegalStateException("Waited too long sounds to synchronise")
            }
        }
    }

    private fun getFileInternal(soundFileName: String): File? {
        val file = File("$SOUNDS_PATH/${soundFileName.trim()}")
        if (file.exists()) {
            return file
        }
        return null
    }

    private fun soundExistsInternal(soundFileName: String): Boolean {
        return getFileInternal(soundFileName) != null
    }

    /** @return a list of local sounds (excluding special ones). */
    private fun getLocalFiles(): List<String> {
        val root = File(SOUNDS_PATH)
        if (!root.exists()) {
            root.mkdirs()
            return emptyList()
        }
        return root.list()?.toList().orEmpty().filter {
            it !in SPECIAL_SOUNDS
        }
    }

    /** Copy special sounds from resources to the destination folder. */
    private fun copySpecialSounds() {
        SPECIAL_SOUNDS.forEach {
            if (!File("$SOUNDS_PATH/$it").exists()) {
                val stream = SoundStore::class.java.classLoader.getResourceAsStream("$SPECIAL_SOUNDS_PATH/$it")
                if (stream != null) {
                    Files.copy(stream, Paths.get("$SOUNDS_PATH/$it"))
                }
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
}
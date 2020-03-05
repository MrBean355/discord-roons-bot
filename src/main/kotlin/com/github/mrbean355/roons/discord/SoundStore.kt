package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.component.PlaySounds
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Folder to store downloaded sounds in. */
private const val SOUNDS_PATH = "sounds"
/** Folder within resources where special sounds live. */
private const val SPECIAL_SOUNDS_PATH = "special_sounds"
/** Special sounds that don't exist on the PlaySounds page. */
private val SPECIAL_SOUNDS = listOf("herewegoagain.mp3", "useyourmidas.mp3", "wefuckinglost.mp3")

@Component
class SoundStore @Autowired constructor(private val playSounds: PlaySounds, private val logger: Logger) {
    private val fileChecksums = mutableMapOf<String, String>()
    private val busySynchronising = AtomicBoolean(false)

    /** @return names of all downloaded sounds. */
    fun listAll(): Map<String, String> {
        assertNotSynchronising()
        return File(SOUNDS_PATH).listFiles()?.toList().orEmpty().associateWith {
            fileChecksums.getOrPut(it.name) { it.checksum() }
        }.mapKeys { it.key.name }
    }

    /** @return [File] for the specified [soundFileName] if it exists, `null` otherwise. */
    fun getFile(soundFileName: String): File? {
        assertNotSynchronising()
        return getFileInternal(soundFileName)
    }

    /** @return `true` if the sound file exists, `false` otherwise. */
    fun soundExists(soundFileName: String): Boolean {
        assertNotSynchronising()
        return soundExistsInternal(soundFileName)
    }

    /**
     * Sync our local sounds with the PlaySounds page.
     * Downloads sounds which don't exist locally.
     * Deletes local sounds which don't exist remotely.
     * Scheduled for once per day.
     */
    @Scheduled(fixedRate = 3_600_000)
    fun synchroniseSounds() {
        busySynchronising.set(true)
        try {
            logger.info("Synchronising sounds")
            val downloaded = AtomicInteger()
            val deleted = AtomicInteger()
            val localFiles = getLocalFiles().toMutableList()
            val remoteFiles = playSounds.listRemoteFiles()

            /* Download all remote files that don't exist locally. */
            remoteFiles.forEach {
                localFiles.remove(it.fileName)
                if (!soundExistsInternal(it.fileName)) {
                    playSounds.downloadFile(it, SOUNDS_PATH)
                    downloaded.incrementAndGet()
                    logger.info("Downloaded: ${it.fileName}")
                }
            }
            /* Delete local files that don't exist remotely. */
            localFiles.forEach {
                File("$SOUNDS_PATH/$it").delete()
                deleted.incrementAndGet()
                logger.info("Deleted old sound: $it")
            }
            copySpecialSounds()
        } finally {
            busySynchronising.set(false)
        }
    }

    private fun assertNotSynchronising() {
        check(!busySynchronising.get()) {
            "Cannot get sound files while synchronising"
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
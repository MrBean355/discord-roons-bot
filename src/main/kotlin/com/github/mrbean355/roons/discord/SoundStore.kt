package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.component.PlaySounds
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

/** Folder to store downloaded sounds in. */
private const val SOUNDS_PATH = "sounds"
/** Folder within resources where special sounds live. */
private const val SPECIAL_SOUNDS_PATH = "special_sounds"
/** Special sounds that don't exist on the PlaySounds page. */
private val SPECIAL_SOUNDS = listOf("herewegoagain.mp3", "useyourmidas.wav", "wefuckinglost.wav")

@Component
class SoundStore @Autowired constructor(private val playSounds: PlaySounds, private val logger: Logger) {

    /** @return the relative path on the system to the specified sound. */
    fun getFilePath(soundFileName: String): String {
        return "$SOUNDS_PATH/${soundFileName.trim()}"
    }

    /** @return `true` if the sound file exists, `false` otherwise. */
    fun soundExists(soundFileName: String): Boolean {
        if (soundFileName.isBlank()) {
            return false
        }
        return File(getFilePath(soundFileName)).exists()
    }

    /**
     * Sync our local sounds with the PlaySounds page.
     * Downloads sounds which don't exist locally.
     * Deletes local sounds which don't exist remotely.
     * Scheduled for once per day.
     */
    @Scheduled(fixedRate = 86_400_000)
    fun synchroniseSounds() {
        logger.info("Synchronising sounds")
        val downloaded = AtomicInteger()
        val deleted = AtomicInteger()
        val localFiles = getLocalFiles().toMutableList()
        val remoteFiles = playSounds.listRemoteFiles()

        /* Download all remote files that don't exist locally. */
        remoteFiles.forEach {
            localFiles.remove(it.fileName)
            if (!soundExists(it.fileName)) {
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
}
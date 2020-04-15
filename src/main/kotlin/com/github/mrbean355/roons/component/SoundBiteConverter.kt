package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.telegram.TelegramNotifier
import org.slf4j.Logger
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.TimeUnit

private const val UNIX_EXECUTABLE = "ffmpeg"
private const val WINDOWS_EXECUTABLE = "ffmpeg.exe"

/** Convert downloaded sound bites to a consistent MP3 format. */
@Component
class SoundBiteConverter(private val logger: Logger, private val telegramNotifier: TelegramNotifier) {
    private val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
    private val ffmpegPath: String

    init {
        ffmpegPath = if (isWindows) {
            // Assume that FFMPEG is added to the PATH.
            // Windows is only used for dev, so we don't need to bundle the executable with the JAR.
            WINDOWS_EXECUTABLE
        } else {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "roons")
            File(tempDir, UNIX_EXECUTABLE).absolutePath
        }
        ensureInstalled()
    }

    fun convert(victim: File) {
        if (!ensureInstalled()) {
            logger.warn("Skipping MP3 conversion; FFMPEG not copied")
            telegramNotifier.sendMessage("⚠️ Can't convert <b>${victim.name}</b>; FFMPEG not installed.")
            return
        }
        val victimPath = victim.absolutePath
        val parentDir = File(victimPath.substringBeforeLast(File.separatorChar))
        val convertedName = victim.nameWithoutExtension + ".mp3"
        val tempOutputName = "tmp_$convertedName"
        val tempOutputFile = File(parentDir, tempOutputName)
        val exitCode = runCommand(ffmpegPath, "-i", victimPath, tempOutputFile.absolutePath)
        if (exitCode != 0) {
            logger.error("Failed to convert $victim, exited with: $exitCode")
            telegramNotifier.sendMessage("⚠️ Can't convert <b>${victim.name}</b>; FFMPEG failed; code=$exitCode.")
            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }
            return
        }
        victim.delete()
        tempOutputFile.renameTo(File(parentDir, convertedName))
    }

    private fun ensureInstalled(): Boolean {
        if (isWindows || File(ffmpegPath).exists()) {
            return true
        }
        return synchronized(this) {
            File(ffmpegPath).exists() || installOnUnix()
        }
    }

    /**
     * On Unix, we copy the bundled executable to a temp directory.
     */
    private fun installOnUnix(): Boolean {
        // Create temp FFMPEG directory
        val dir = File(ffmpegPath.substringBeforeLast(File.separatorChar))
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdirs()
        dir.deleteOnExit()

        // Copy FFMPEG executable
        val exe = ffmpegPath.substringAfterLast(File.separatorChar)
        val target = File(dir, exe)
        SoundBiteConverter::class.java.classLoader.getResourceAsStream(exe)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (!target.exists()) {
            logger.error("Failed to copy '$exe' to: ${dir.absolutePath}")
            telegramNotifier.sendMessage("⚠️ Failed to copy FFMPEG.")
            return false
        }

        val ffmpegPath = target.absolutePath
        logger.info("Copied $exe to $ffmpegPath")
        val exitCode = runCommand("/bin/chmod", "755", ffmpegPath)
        return if (exitCode == 0) {
            telegramNotifier.sendMessage("✔️ Successfully copied FFMPEG.")
            true
        } else {
            telegramNotifier.sendMessage("⚠️ Couldn't make FFMPEG executable; code=$exitCode")
            false
        }
    }

    private fun runCommand(vararg arg: String): Int {
        val process = ProcessBuilder(*arg)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        process.waitFor(1, TimeUnit.HOURS)
        val exitValue = process.exitValue()
        if (exitValue != 0) {
            logger.error(process.inputStream.bufferedReader().readText().trim())
        }
        return process.exitValue()
    }
}
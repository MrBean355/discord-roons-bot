package com.github.mrbean355.roons.component

import org.slf4j.Logger
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.TimeUnit

/** Convert downloaded sound bites to a consistent MP3 format. */
@Component
class SoundBiteConverter(private val logger: Logger) {
    private val ffmpegPath: String

    init {
        val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
        ffmpegPath = if (isWindows) initWindows() else initUnix()
    }

    fun convert(victim: File) {
        if (ffmpegPath.isBlank()) {
            logger.warn("Skipping MP3 conversion; FFMPEG not copied")
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
            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }
            return
        }
        victim.delete()
        tempOutputFile.renameTo(File(parentDir, convertedName))
    }

    /**
     * On Windows we hope that FFMPEG is added to the current PATH.
     * Windows is only used for dev, so we don't need to bundle the executable (reduces JAR size).
     */
    private fun initWindows() = "ffmpeg.exe"

    /**
     * On Unix, we copy the bundled executable to a temp directory.
     */
    private fun initUnix(): String {
        // Create temp FFMPEG directory
        val dir = File(System.getProperty("java.io.tmpdir"), "roons")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdirs()
        dir.deleteOnExit()

        // Copy FFMPEG executable
        val exe = "ffmpeg"
        val target = File(dir, exe)
        SoundBiteConverter::class.java.classLoader.getResourceAsStream(exe)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (!target.exists()) {
            logger.error("Failed to copy '$exe' to: ${dir.absolutePath}")
            return ""
        }

        val ffmpegPath = target.absolutePath
        logger.info("Copied $exe to $ffmpegPath")
        runCommand("/bin/chmod", "755", ffmpegPath)
        return ffmpegPath
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
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

package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.discord.FILE_EXTENSION
import org.slf4j.Logger
import org.springframework.stereotype.Component
import java.io.File
import kotlin.math.roundToInt

private const val UNIX_EXECUTABLE = "ffmpeg"
private const val WINDOWS_EXECUTABLE = "ffmpeg.exe"

/** Convert downloaded sound bites to a consistent MP3 format. */
@Component
class SoundBiteConverter(private val logger: Logger) {
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

    fun convert(victim: File, volume: Int) {
        if (!ensureInstalled()) {
            logger.warn("Skipping MP3 conversion; FFMPEG not copied")
            return
        }
        val victimPath = victim.absolutePath
        val parentDir = File(victimPath.substringBeforeLast(File.separatorChar))
        val convertedName = victim.nameWithoutExtension + ".$FILE_EXTENSION"
        val tempOutputName = "tmp_$convertedName"
        val tempOutputFile = File(parentDir, tempOutputName)
        val exitCode = SystemCommands.execute(ffmpegPath, "-i", victimPath, tempOutputFile.absolutePath, "-vol", volume.transformVolume())
        if (exitCode != 0) {
            logger.error("Failed to convert $victim, exited with: $exitCode")
            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }
            throw IllegalStateException("Failed to convert $victim, exited with: $exitCode")
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
            return false
        }

        val ffmpegPath = target.absolutePath
        logger.info("Copied $exe to $ffmpegPath")
        val exitCode = SystemCommands.execute("/bin/chmod", "755", ffmpegPath)
        return exitCode == 0
    }

    private fun Int.transformVolume(): String {
        // PlaySounds page has a max volume of 100.
        // FFMPEG uses 256 for the 'normal' volume.
        return times(2.56).roundToInt().toString()
    }
}
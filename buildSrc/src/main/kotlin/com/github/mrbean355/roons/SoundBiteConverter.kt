package com.github.mrbean355.roons

import org.gradle.api.Project
import java.io.File
import java.util.concurrent.TimeUnit

internal object SoundBiteConverter {

    fun convert(project: Project, input: File, volume: Int, outputName: String) {
        val ffmpeg = File(project.rootProject.file("buildSrc"), "ffmpeg.exe").absolutePath
        val parentDir = input.parentFile
        val tempOutputFile = File(parentDir, "tmp_$outputName")
        val exitCode = execute(ffmpeg, "-i", input.absolutePath, "-filter:a", "volume=${volume.transformVolume()}", tempOutputFile.absolutePath)
        if (exitCode != 0) {
            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }
            throw IllegalStateException("Failed to convert $input, exited with: $exitCode")
        }
        input.delete()
        tempOutputFile.renameTo(File(parentDir, outputName))
    }

    private fun Int.transformVolume(): Double {
        // PlaySounds page has a max volume of 100.
        // FFMPEG uses 1.0 for the 'normal' volume.
        return div(100.0)
    }

    private fun execute(vararg arg: String): Int {
        val process = ProcessBuilder(*arg)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(1, TimeUnit.HOURS)
        val exitValue = process.exitValue()
        if (exitValue != 0) {
            println(process.inputStream.bufferedReader().readText().trim())
        }
        return process.exitValue()
    }
}

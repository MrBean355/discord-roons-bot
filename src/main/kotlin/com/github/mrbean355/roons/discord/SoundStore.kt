package com.github.mrbean355.roons.discord

import java.io.File

object SoundStore {

    /** @return the relative path on the system to the specified sound. */
    fun getFilePath(soundFileName: String): String {
        return "sounds/${soundFileName.trim()}"
    }

    /** @return `true` if the sound file exists, `false` otherwise. */
    fun soundExists(soundFileName: String): Boolean {
        if (soundFileName.isBlank()) {
            return false
        }
        return File(getFilePath(soundFileName)).exists()
    }
}
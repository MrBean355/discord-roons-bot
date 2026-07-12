package com.github.mrbean355.roons.discord

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

internal class SoundStoreTest {

    @Test
    fun testManifestContainsAllSoundsInDirectory() {
        val soundsDir = File("src/main/resources/sounds")
        assertTrue(soundsDir.exists(), "sounds directory should exist")
        assertTrue(soundsDir.isDirectory, "sounds should be a directory")

        // 1. Get all MP3 files in the directory
        val mp3Files = soundsDir.listFiles()
            ?.filter { it.extension.lowercase() == "mp3" }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()

        // 2. Read the manifest file
        val manifestFile = File(soundsDir, "manifest.json")
        assertTrue(manifestFile.exists(), "manifest.json should exist")

        val manifestSounds = manifestFile.readText()
            .let { Json.decodeFromString<List<String>>(it) }
            .toSet()

        // 3. Verify they match exactly
        val missingFromManifest = mp3Files - manifestSounds
        assertTrue(missingFromManifest.isEmpty(), "Sound files in directory but missing from manifest.json: $missingFromManifest")

        val extraInManifest = manifestSounds - mp3Files
        assertTrue(extraInManifest.isEmpty(), "Sound files in manifest.json but missing from directory: $extraInManifest")

        assertEquals(mp3Files.size, manifestSounds.size, "Count of files in directory and manifest should be equal")
    }
}

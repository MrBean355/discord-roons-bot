package com.github.mrbean355.roons

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.File

private const val PLAY_SOUNDS_URL = "https://chatbot.admiralbulldog.live/playsounds"

internal object PlaySoundsService {
    private val client = HttpClient(Apache5)

    suspend fun listSounds(): List<SoundBite> {
        val response = client.get(PLAY_SOUNDS_URL)
        check(response.status.isSuccess()) {
            "Failed to list sounds: ${response.status}"
        }

        return response.body<String>().split("class=\"category\"")
            .drop(1)
            .flatMap(::processCategory)
    }

    suspend fun downloadSound(soundFile: SoundBite, directory: File): File {
        val response = client.get(soundFile.url)
        check(response.status.isSuccess()) {
            "Failed to download ${soundFile.url}: ${response.status}"
        }

        return File(directory, soundFile.name)
            .apply { writeBytes(response.body()) }
    }

    private fun processCategory(html: String): List<SoundBite> {
        return html.split("<div class=\"play-in-browser-wrapper\"")
            .drop(1)
            .map { block ->
                val content = block.trim().substringBefore("</div>")
                SoundBite(
                    name = content.split("data-name=\"")[1].substringBefore('\"'),
                    url = content.split("data-link=\"")[1].substringBefore('\"'),
                    volume = content.split("data-volume=\"")[1].substringBefore('\"').toInt(),
                )
            }
    }
}
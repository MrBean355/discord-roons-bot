/*
 * Copyright 2021 Michael Johnston
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.io.File
import java.io.FileOutputStream

private const val PLAY_SOUNDS_URL = "http://chatbot.admiralbulldog.live/playsounds"

@Component
class PlaySounds(
    private val soundBiteConverter: SoundBiteConverter,
    private val listRestTemplate: RestTemplate,
    private val downloadRestTemplate: RestTemplate,
) {

    @Autowired
    constructor(soundBiteConverter: SoundBiteConverter) : this(
        soundBiteConverter,
        RestTemplateBuilder()
            .messageConverters(StringHttpMessageConverter())
            .build(),
        RestTemplate()
    )

    /** Scrape the PlaySounds web page and collect a list of sound file names and URLs. */
    fun listRemoteFiles(): List<RemoteSoundFile> {
        val response = listRestTemplate.getForEntity<String>(PLAY_SOUNDS_URL)
        val responseBody = response.body
        if (response.statusCode != HttpStatus.OK || responseBody == null) {
            throw RuntimeException("Error downloading PlaySounds page. Response code: ${response.statusCodeValue}")
        }

        val blocks = responseBody.split("<div class=\"play-in-browser-wrapper\"")
            .drop(1)

        return blocks.map { block ->
            val content = block.trim().substringBefore("</div>")
            RemoteSoundFile(
                name = content.split("data-name=\"")[1].substringBefore('\"'),
                url = content.split("data-link=\"")[1].substringBefore('\"'),
                volume = content.split("data-volume=\"")[1].substringBefore('\"').toInt(),
            )
        }
    }

    /** Download the given sound to the given destination. */
    fun downloadFile(file: RemoteSoundFile, destination: String) {
        val response = downloadRestTemplate.getForEntity<ByteArray>(file.url)
        val responseBody = response.body
        if (response.statusCode != HttpStatus.OK || responseBody == null) {
            throw RuntimeException("Error downloading file: ${file.name}. Response code: ${response.statusCodeValue}")
        }

        val filePath = "$destination/${file.name}"

        responseBody.inputStream().use { input ->
            FileOutputStream(filePath).use { output ->
                input.copyTo(output)
            }
        }

        soundBiteConverter.convert(File(filePath), file.volume)
    }

    data class RemoteSoundFile(
        val name: String,
        val url: String,
        val volume: Int
    )
}


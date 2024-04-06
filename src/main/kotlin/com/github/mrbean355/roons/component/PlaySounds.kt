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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

private const val PLAY_SOUNDS_URL = "https://chatbot.admiralbulldog.live/playsounds"

@Component
class PlaySounds(
    private val listRestTemplate: RestTemplate,
    private val downloadRestTemplate: RestTemplate,
) {

    @Autowired
    constructor() : this(
        RestTemplateBuilder()
            .messageConverters(StringHttpMessageConverter())
            .build(),
        RestTemplate()
    )

    /** Scrape the PlaySounds web page and collect a list of available sound bites. */
    fun listRemoteFiles(): List<RemoteSoundFile> {
        val response = listRestTemplate.getForEntity<String>(PLAY_SOUNDS_URL)
        val responseBody = response.body
        if (response.statusCode != HttpStatus.OK || responseBody == null) {
            throw RuntimeException("Error downloading PlaySounds page. Response code: ${response.statusCode}")
        }

        return responseBody.split("class=\"category\"")
            .drop(1)
            .flatMap(::processCategory)
    }

    fun downloadFile(file: RemoteSoundFile): ByteArray {
        val response = downloadRestTemplate.getForEntity<ByteArray>(file.url)
        val responseBody = response.body
        if (response.statusCode != HttpStatus.OK || responseBody == null) {
            throw RuntimeException("Error downloading file: ${file.name}. Response code: ${response.statusCode}")
        }

        return responseBody
    }

    private fun processCategory(html: String): List<RemoteSoundFile> {
        val category = html.drop(1)
            .substringBefore("</h3>")
            .replace("&#39;", "'")

        return html.split("<div class=\"play-in-browser-wrapper\"")
            .drop(1)
            .map { block ->
                val content = block.trim().substringBefore("</div>")
                RemoteSoundFile(
                    name = content.split("data-name=\"")[1].substringBefore('\"'),
                    url = content.split("data-link=\"")[1].substringBefore('\"'),
                    volume = content.split("data-volume=\"")[1].substringBefore('\"').toInt(),
                    category = category,
                )
            }
    }

    data class RemoteSoundFile(
        val name: String,
        val url: String,
        val volume: Int,
        val category: String,
    )
}


package com.github.mrbean355.roons.component

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream

private const val PLAY_SOUNDS_URL = "http://chatbot.admiralbulldog.live/playsounds"

@Component
class PlaySounds(private val soundBiteConverter: SoundBiteConverter) {

    /** Scrape the PlaySounds web page and collect a list of sound file names and URLs. */
    fun listRemoteFiles(): List<RemoteSoundFile> {
        val restTemplate = RestTemplateBuilder()
                .messageConverters(StringHttpMessageConverter())
                .build()
        val response = restTemplate.getForEntity(PLAY_SOUNDS_URL, String::class.java)
        val responseBody = response.body
        if (response.statusCode != HttpStatus.OK || responseBody == null) {
            throw RuntimeException("Unable to download HTML, response=$response")
        }
        val blocks = responseBody.split("<tr>")
                .drop(2)

        return blocks.map { block ->
            val friendlyName = block.split("<td>")[1].split("</td>").first()
            val url = block.split("data-link=\"")[1].split("\"").first()
            val fileExtension = url.substringAfterLast('.', missingDelimiterValue = "")
            val fileName = "$friendlyName.$fileExtension"
            RemoteSoundFile(fileName, url)
        }
    }

    /** Download the given sound to the given destination. */
    fun downloadFile(file: RemoteSoundFile, destination: String) {
        val response = RestTemplate().getForEntity(file.url, ByteArray::class.java)
        val responseBody = response.body
        if (response.statusCode != HttpStatus.OK || responseBody == null) {
            throw RuntimeException("Unable to download $file, response=$response")
        }
        val filePath = "$destination/${file.fileName}"
        val stream = responseBody.inputStream()
        val output = FileOutputStream(filePath)
        val buffer = ByteArray(4096)
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) {
                break
            }
            output.write(buffer, 0, read)
        }
        output.close()
        stream.close()
        soundBiteConverter.convert(File(filePath))
    }

    data class RemoteSoundFile(val fileName: String, val url: String) {
        val localFileName = fileName.substringBeforeLast('.') + ".mp3"
    }
}


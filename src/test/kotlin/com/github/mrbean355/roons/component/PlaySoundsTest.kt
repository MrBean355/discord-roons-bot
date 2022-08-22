/*
 * Copyright 2022 Michael Johnston
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.mrbean355.roons.loadTestResource
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.io.File

private const val EXPECTED_URL = "https://chatbot.admiralbulldog.live/playsounds"

internal class PlaySoundsTest {
    @MockK
    private lateinit var soundBiteConverter: SoundBiteConverter

    @MockK
    private lateinit var listRestTemplate: RestTemplate

    @MockK
    private lateinit var downloadRestTemplate: RestTemplate
    private lateinit var playSounds: PlaySounds

    @TempDir
    lateinit var destination: File

    @BeforeEach
    internal fun setUp() {
        val l = LoggerFactory.getLogger("io.mockk.impl.recording.states.AnsweringState") as Logger
        l.level = Level.OFF

        MockKAnnotations.init(this)
        every { listRestTemplate.getForEntity<String>(EXPECTED_URL) } returns mockk {
            every { statusCode } returns HttpStatus.OK
            every { body } returns loadTestResource("playsounds-response.html")
        }
        every { downloadRestTemplate.getForEntity<ByteArray>(any<String>()) } returns mockk {
            every { statusCode } returns HttpStatus.OK
            every { body } returns byteArrayOf()
        }
        playSounds = PlaySounds(soundBiteConverter, listRestTemplate, downloadRestTemplate)
    }

    @Test
    internal fun testListRemoteFiles_CallsRestTemplate() {
        playSounds.listRemoteFiles()

        verify { listRestTemplate.getForEntity<String>(EXPECTED_URL) }
    }

    @Test
    internal fun testListRemoteFiles_UnsuccessfulResponse_ThrowsException() {
        every { listRestTemplate.getForEntity<String>(EXPECTED_URL) } returns mockk {
            every { statusCode } returns HttpStatus.SERVICE_UNAVAILABLE
            every { body } returns null
        }

        assertThrows<RuntimeException> {
            playSounds.listRemoteFiles()
        }
    }

    @Test
    internal fun testListRemoteFiles_SuccessfulResponseWithNullBody_ThrowsException() {
        every { listRestTemplate.getForEntity<String>(EXPECTED_URL) } returns mockk {
            every { statusCode } returns HttpStatus.OK
            every { body } returns null
        }

        assertThrows<RuntimeException> {
            playSounds.listRemoteFiles()
        }
    }

    @Test
    internal fun testListRemoteFiles_SuccessfulResponseWithBody_ReturnsCorrectItems() {
        val result = playSounds.listRemoteFiles()

        assertEquals(4, result.size)
        assertEquals("eight", result[0].name)
        assertEquals("https://admiralbullbot.github.io/playsounds/files/bulldog/8.ogg", result[0].url)
        assertEquals("eleven", result[1].name)
        assertEquals("https://admiralbullbot.github.io/playsounds/files/bulldog/11.ogg", result[1].url)
        assertEquals("ahaha4head", result[2].name)
        assertEquals("https://admiralbullbot.github.io/playsounds/files/bulldog/ahaha4head.ogg", result[2].url)
        assertEquals("alchemist", result[3].name)
        assertEquals("https://admiralbullbot.github.io/playsounds/files/bulldog/alchemist.ogg", result[3].url)
    }

    @Test
    internal fun testDownloadFile_CallsRestTemplate() {
        justRun { soundBiteConverter.convert(any(), any()) }

        playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50), destination.absolutePath)

        verify { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") }
    }

    @Test
    internal fun testDownloadFile_UnsuccessfulResponse_ThrowsException() {
        every { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") } returns mockk {
            every { statusCode } returns HttpStatus.SERVICE_UNAVAILABLE
            every { body } returns null
        }

        assertThrows<RuntimeException> {
            playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50), destination.absolutePath)
        }
    }

    @Test
    internal fun testDownloadFile_SuccessfulResponseWithNullBody_ThrowsException() {
        every { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") } returns mockk {
            every { statusCode } returns HttpStatus.OK
            every { body } returns null
        }

        assertThrows<RuntimeException> {
            playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50), destination.absolutePath)
        }
    }

    @Test
    internal fun testDownloadFile_SuccessfulResponseWithNonNullBody_ConvertsFile() {
        every { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") } returns mockk {
            every { statusCode } returns HttpStatus.OK
            every { body } returns File("src/test/resources/roons.mp3").readBytes()
        }
        justRun { soundBiteConverter.convert(any(), any()) }

        playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50), destination.absolutePath)

        val slot = slot<File>()
        verify { soundBiteConverter.convert(capture(slot), 50) }
        assertEquals(File(destination, "roons").absolutePath, slot.captured.path)
    }
}
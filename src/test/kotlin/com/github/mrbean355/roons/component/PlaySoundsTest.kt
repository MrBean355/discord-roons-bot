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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.mrbean355.roons.loadTestResource
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.io.File

private const val EXPECTED_URL = "https://chatbot.admiralbulldog.live/playsounds"

internal class PlaySoundsTest {
    @MockK
    private lateinit var listRestTemplate: RestTemplate

    @MockK
    private lateinit var downloadRestTemplate: RestTemplate
    private lateinit var playSounds: PlaySounds

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
        playSounds = PlaySounds(listRestTemplate, downloadRestTemplate)
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

        assertEquals(625, result.size)

        with(result[159]) {
            assertEquals("roons", name)
            assertEquals("https://admiralbullbot.github.io/playsounds/files/bulldog/roons.ogg", url)
            assertEquals(30, volume)
            assertEquals("Bulldog's voice", category)
        }

        with(result[78]) {
            assertEquals("eel", name)
            assertEquals("https://admiralbullbot.github.io/playsounds/files/bulldog/eel.ogg", url)
            assertEquals(35, volume)
            assertEquals("Bulldog's voice", category)
        }

        with(result[387]) {
            assertEquals("weed", name)
            assertEquals("https://admiralbullbot.github.io/playsounds/files/new/weed.ogg", url)
            assertEquals(50, volume)
            assertEquals("Songs", category)
        }

        with(result[473]) {
            assertEquals("vivon", name)
            assertEquals("https://admiralbullbot.github.io/playsounds/files/bulldog/vivon.ogg", url)
            assertEquals(37, volume)
            assertEquals("Ugandan", category)
        }
    }

    @Test
    internal fun testDownloadFile_CallsRestTemplate() {
        playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50, ""))

        verify { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") }
    }

    @Test
    internal fun testDownloadFile_UnsuccessfulResponse_ThrowsException() {
        every { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") } returns mockk {
            every { statusCode } returns HttpStatus.SERVICE_UNAVAILABLE
            every { body } returns null
        }

        assertThrows<RuntimeException> {
            playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50, ""))
        }
    }

    @Test
    internal fun testDownloadFile_SuccessfulResponseWithNullBody_ThrowsException() {
        every { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") } returns mockk {
            every { statusCode } returns HttpStatus.OK
            every { body } returns null
        }

        assertThrows<RuntimeException> {
            playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50, ""))
        }
    }

    @Test
    internal fun testDownloadFile_SuccessfulResponseWithNonNullBody_ConvertsFile() {
        val testBytes = File("src/test/resources/roons.mp3").readBytes()
        every { downloadRestTemplate.getForEntity<ByteArray>("https://roons.mp3") } returns mockk {
            every { statusCode } returns HttpStatus.OK
            every { body } returns testBytes
        }

        val result = playSounds.downloadFile(PlaySounds.RemoteSoundFile("roons", "https://roons.mp3", 50, ""))

        assertSame(testBytes, result)
    }
}
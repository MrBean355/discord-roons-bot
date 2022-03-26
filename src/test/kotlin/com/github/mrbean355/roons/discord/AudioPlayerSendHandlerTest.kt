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

package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

internal class AudioPlayerSendHandlerTest {
    @MockK
    private lateinit var audioPlayer: AudioPlayer

    @MockK
    private lateinit var byteBuffer: ByteBuffer

    @MockK
    private lateinit var mutableAudioFrame: MutableAudioFrame
    private lateinit var audioSendHandler: AudioPlayerSendHandler

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        justRun { mutableAudioFrame.setBuffer(byteBuffer) }
        audioSendHandler = AudioPlayerSendHandler(audioPlayer, byteBuffer, mutableAudioFrame)
    }

    @Test
    internal fun testConstruction_SetsBuffer() {
        verify { mutableAudioFrame.setBuffer(byteBuffer) }
    }

    @Test
    internal fun testCanProvide_CallsAudioPlayer() {
        every { audioPlayer.provide(any()) } returns false

        audioSendHandler.canProvide()

        verify { audioPlayer.provide(mutableAudioFrame) }
    }

    @Test
    internal fun testCanProvide_AudioPlayerReturnsFalse_ReturnsFalse() {
        every { audioPlayer.provide(any()) } returns false

        val result = audioSendHandler.canProvide()

        assertFalse(result)
    }

    @Test
    internal fun testCanProvide_AudioPlayerReturnsTrue_ReturnsTrue() {
        every { audioPlayer.provide(any()) } returns true

        val result = audioSendHandler.canProvide()

        assertTrue(result)
    }

    @Test
    internal fun testProvide20MsAudio_ReturnsByteBuffer() {
        every { byteBuffer.flip() } returns mockk()

        val result = audioSendHandler.provide20MsAudio()

        assertSame(byteBuffer, result)
    }

    @Test
    internal fun testProvide20MsAudio_CallsFlip() {
        every { byteBuffer.flip() } returns mockk()

        audioSendHandler.provide20MsAudio()

        verify { byteBuffer.flip() }
    }

    @Test
    internal fun testIsOpus_ReturnsTrue() {
        val result = audioSendHandler.isOpus

        assertTrue(result)
    }
}
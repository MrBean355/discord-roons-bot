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

package com.github.mrbean355.roons.discord.audio

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.OpusAudioDataFormat
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TrackSchedulerTest {
    @MockK(relaxed = true)
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var trackScheduler: TrackScheduler

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        trackScheduler = TrackScheduler(audioPlayer)
    }

    @Test
    internal fun testQueue_NoPlayingTrack_StartsTrack() {
        val track = mockk<AudioTrack>()
        every { audioPlayer.playingTrack } returns null

        trackScheduler.queue(track, 55, 110)

        verify {
            audioPlayer.volume = 55
            audioPlayer.setFilterFactory(any<TrackScheduler.RateFilterFactory>())
            audioPlayer.startTrack(track, false)
        }
    }

    @Test
    internal fun testQueue_HasPlayingTrack_DoesNotStartTrack() {
        val track = mockk<AudioTrack>()
        every { audioPlayer.playingTrack } returns mockk()

        trackScheduler.queue(track, 55, 110)

        verify(inverse = true) {
            audioPlayer.volume = any()
            audioPlayer.setFilterFactory(any<TrackScheduler.RateFilterFactory>())
            audioPlayer.startTrack(any(), any())
        }
    }

    @Test
    internal fun testOnTrackEnd_MayStartNextIsFalse_DoesNothing() {
        trackScheduler.onTrackEnd(audioPlayer, mockk(), AudioTrackEndReason.CLEANUP)

        verify(inverse = true) {
            audioPlayer.volume = any()
            audioPlayer.setFilterFactory(any<TrackScheduler.RateFilterFactory>())
            audioPlayer.startTrack(any(), any())
        }
    }

    @Test
    internal fun testOnTrackEnd_MayStartNextIsTrue_NoQueuedTrack_DoesNotStartTrack() {
        trackScheduler.onTrackEnd(audioPlayer, mockk(), AudioTrackEndReason.FINISHED)

        verify(inverse = true) {
            audioPlayer.volume = any()
            audioPlayer.setFilterFactory(any<TrackScheduler.RateFilterFactory>())
            audioPlayer.startTrack(any(), any())
        }
    }

    @Test
    internal fun testOnTrackEnd_MayStartNextIsTrue_HasQueuedTrack_StartsNextTrack() {
        every { audioPlayer.playingTrack } returns mockk()
        val queuedTrack = mockk<AudioTrack>()
        trackScheduler.queue(queuedTrack, 75, 85)

        trackScheduler.onTrackEnd(audioPlayer, mockk(), AudioTrackEndReason.FINISHED)

        verify {
            audioPlayer.volume = 75
            audioPlayer.setFilterFactory(any<TrackScheduler.RateFilterFactory>())
            audioPlayer.startTrack(queuedTrack, false)
        }
    }

    @Test
    internal fun testRateFilterFactory() {
        val factory = TrackScheduler.RateFilterFactory(0.33)
        val format = OpusAudioDataFormat(4, 44000, 0)

        val filters = factory.buildChain(mockk(), format, mockk())

        assertEquals(1, filters.size)
        assertTrue(filters[0] is TimescalePcmAudioFilter)
        val filter = filters[0] as TimescalePcmAudioFilter
        assertEquals(0.33, filter.rate)
    }
}
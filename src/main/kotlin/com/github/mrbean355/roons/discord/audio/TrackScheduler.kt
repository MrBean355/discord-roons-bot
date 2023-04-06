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

package com.github.mrbean355.roons.discord.audio

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    private val queue = LinkedBlockingQueue<QueuedTrack>()

    fun queue(track: AudioTrack, volume: Int, rate: Int) {
        synchronized(this) {
            if (player.playingTrack == null) {
                startTrack(track, volume, rate)
            } else {
                queue.offer(QueuedTrack(track, volume, rate))
            }
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack?, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            synchronized(this) {
                queue.poll()?.let { next ->
                    startTrack(next.track, next.volume, next.rate)
                }
            }
        }
    }

    private fun startTrack(track: AudioTrack, volume: Int, rate: Int) {
        player.volume = volume
        player.setFilterFactory(RateFilterFactory(rate / 100.0))
        player.startTrack(track, false)
    }

    private class QueuedTrack(
        val track: AudioTrack,
        val volume: Int,
        val rate: Int
    )

    @VisibleForTesting
    class RateFilterFactory(private val rate: Double) : PcmFilterFactory {

        override fun buildChain(track: AudioTrack?, format: AudioDataFormat, output: UniversalPcmAudioFilter): List<AudioFilter> {
            return listOf(
                TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate)
                    .setRate(rate)
            )
        }
    }
}
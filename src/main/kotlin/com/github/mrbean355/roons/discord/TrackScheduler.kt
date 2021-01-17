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

package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
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

    // TODO: Figure out how to change the playback rate.
    private fun startTrack(track: AudioTrack, volume: Int, @Suppress("UNUSED_PARAMETER") rate: Int) {
        player.volume = volume
        player.startTrack(track, false)
    }

    private class QueuedTrack(
            val track: AudioTrack,
            val volume: Int,
            val rate: Int
    )
}
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
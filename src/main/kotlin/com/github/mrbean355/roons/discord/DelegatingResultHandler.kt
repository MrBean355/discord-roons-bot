package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

/** Plays loaded tracks with an [AudioPlayer]. */
class DelegatingResultHandler(private val player: AudioPlayer)
    : AudioLoadResultHandler {

    override fun trackLoaded(track: AudioTrack) {
        player.playTrack(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist?) {}

    override fun noMatches() {
        println("No matches found.")
    }

    override fun loadFailed(exception: FriendlyException?) {
        println("Load failed: $exception")
    }
}
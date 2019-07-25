package com.github.mrbean355.roons.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import discord4j.voice.AudioProvider
import discord4j.voice.VoiceConnection

class GuildPlayerManager(audioPlayerManager: AudioPlayerManager) {
    val audioPlayer: AudioPlayer = audioPlayerManager.createPlayer()
    val audioProvider: AudioProvider = LavaAudioProvider(audioPlayer)
    var voiceConnection: VoiceConnection? = null

    init {
        audioPlayer.volume = 25
    }
}
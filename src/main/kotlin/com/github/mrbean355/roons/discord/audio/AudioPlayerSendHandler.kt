package com.github.mrbean355.roons.discord.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import org.jetbrains.annotations.VisibleForTesting
import java.nio.ByteBuffer

class AudioPlayerSendHandler @VisibleForTesting constructor(
    private val audioPlayer: AudioPlayer,
    private val buffer: ByteBuffer,
    private val frame: MutableAudioFrame
) : AudioSendHandler {

    constructor(audioPlayer: AudioPlayer) : this(audioPlayer, ByteBuffer.allocate(1024), MutableAudioFrame())

    init {
        frame.setBuffer(buffer)
    }

    override fun canProvide(): Boolean {
        return audioPlayer.provide(frame)
    }

    override fun provide20MsAudio(): ByteBuffer? {
        buffer.flip()
        return buffer
    }

    override fun isOpus() = true

}
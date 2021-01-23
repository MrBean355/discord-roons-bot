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

import com.google.common.annotations.VisibleForTesting
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
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
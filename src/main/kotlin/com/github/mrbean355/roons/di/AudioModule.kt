package com.github.mrbean355.roons.di

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AudioModule {

    @Provides
    @Singleton
    fun provideAudioPlayerManager(): AudioPlayerManager {
        return DefaultAudioPlayerManager()
    }
}
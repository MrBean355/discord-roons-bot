package com.github.mrbean355.roons.di

import dagger.Module
import dagger.Provides
import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence

@Module
class ClientModule(private val apiToken: String) {

    @Provides
    fun provideDiscordClient(): DiscordClient {
        return DiscordClientBuilder(apiToken)
                .setInitialPresence(Presence.online(Activity.playing("Get the roons!")))
                .build()
    }
}
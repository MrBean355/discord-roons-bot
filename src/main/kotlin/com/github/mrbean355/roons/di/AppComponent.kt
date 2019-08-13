package com.github.mrbean355.roons.di

import com.github.mrbean355.roons.discord.RunesDiscordBot
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CommandModule::class, ClientModule::class, AudioModule::class])
interface AppComponent {
    fun discordBot(): RunesDiscordBot
}

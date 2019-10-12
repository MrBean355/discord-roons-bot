package com.github.mrbean355.roons.component

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
object BeanProvider {
    var token = ""

    @Bean
    fun discordClient(): DiscordClient {
        return DiscordClientBuilder(token)
                .setInitialPresence(Presence.online(Activity.playing("Get the roons!")))
                .build()
    }

    @Bean
    fun audioPlayerManager(): AudioPlayerManager {
        return DefaultAudioPlayerManager()
    }

    @Bean
    fun logger(injectionPoint: InjectionPoint): Logger {
        val clazz = injectionPoint.methodParameter?.containingClass
                ?: injectionPoint.field?.declaringClass
        return LoggerFactory.getLogger(clazz)
    }
}
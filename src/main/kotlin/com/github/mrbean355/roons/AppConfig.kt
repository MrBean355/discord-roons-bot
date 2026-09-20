package com.github.mrbean355.roons

import club.minnced.discord.jdave.interop.JDaveSessionFactory
import com.github.mrbean355.roons.discord.DiscordEventHandler
import com.github.mrbean355.roons.security.AdminAuthInterceptor
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.audio.AudioModuleConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient

@Configuration(proxyBeanMethods = false)
class AppConfig(
    private val adminAuthInterceptor: AdminAuthInterceptor? = null
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        if (adminAuthInterceptor != null) {
            registry.addInterceptor(adminAuthInterceptor)
        }
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/terms").setViewName("forward:/terms.html")
        registry.addViewController("/privacy").setViewName("forward:/privacy.html")
    }

    @Bean
    fun jda(
        @Value($$"${DISCORD_API_TOKEN}") token: String,
        discordEventHandler: DiscordEventHandler
    ): JDA = JDABuilder.createDefault(token)
        .addEventListeners(discordEventHandler)
        .setAudioModuleConfig(AudioModuleConfig().withDaveSessionFactory(JDaveSessionFactory()))
        .build()

    @Bean
    fun telegramClient(@Value($$"${TELEGRAM_TOKEN}") token: String): TelegramClient {
        return OkHttpTelegramClient(token)
    }

    @Bean
    @Scope("prototype")
    fun logger(injectionPoint: InjectionPoint): Logger {
        val clazz = injectionPoint.methodParameter?.containingClass
            ?: injectionPoint.field?.declaringClass
        return LoggerFactory.getLogger(clazz)
    }
}

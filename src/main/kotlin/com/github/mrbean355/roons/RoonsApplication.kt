package com.github.mrbean355.roons

import com.github.mrbean355.roons.telegram.ENV_TELEGRAM_TOKEN
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient

@SpringBootApplication
@EnableScheduling
@EnableCaching
class RoonsApplication {

    @Bean
    fun telegramClient(): TelegramClient {
        return OkHttpTelegramClient(System.getenv(ENV_TELEGRAM_TOKEN))
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(RoonsApplication::class.java, *args)
        }
    }
}

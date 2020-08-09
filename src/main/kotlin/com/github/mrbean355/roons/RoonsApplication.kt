package com.github.mrbean355.roons

import com.github.mrbean355.roons.component.BeanProvider
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.telegram.telegrambots.ApiContextInitializer

private const val ENV_TOKEN = "DISCORD_API_TOKEN"

@SpringBootApplication
@EnableScheduling
class RoonsApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val apiToken = System.getenv(ENV_TOKEN)
            if (apiToken.isNullOrBlank()) {
                println("Please set the '$ENV_TOKEN' environment variable to the Discord bot's API token.")
                return
            }
            BeanProvider.setToken(apiToken)
            ApiContextInitializer.init()
            SpringApplication.run(RoonsApplication::class.java, *args)
        }
    }
}

package com.github.mrbean355.roons

import com.github.mrbean355.roons.component.BeanProvider
import com.github.mrbean355.roons.discord.DiscordBot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

private const val ARG_TOKEN = "--bot.token"

@SpringBootApplication
@EnableScheduling
class RoonsApplication @Autowired constructor(@Suppress("unused") private val bot: DiscordBot) {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val botToken = getToken(args)
            if (botToken.isNullOrBlank()) {
                println("Please pass in the Discord bot's API token with the '$ARG_TOKEN' parameter.\n" +
                        "For example: java -jar app.jar $ARG_TOKEN=mytoken123")
                return
            }
            BeanProvider.setToken(botToken)
            SpringApplication.run(RoonsApplication::class.java, *args)
        }

        private fun getToken(args: Array<String>): String? {
            val arg = args.firstOrNull { it.startsWith("$ARG_TOKEN=") } ?: return null
            return arg.split("$ARG_TOKEN=").getOrNull(1)
        }
    }
}

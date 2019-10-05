package com.github.mrbean355.roons

import com.github.mrbean355.roons.beans.BeanProvider
import com.github.mrbean355.roons.discord.RunesDiscordBot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class RoonsApplication @Autowired constructor(bot: RunesDiscordBot) {

    init {
        bot.startAsync()
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val botToken = getToken(args)
            if (botToken.isNullOrBlank()) {
                println("Please pass in only the Discord bot's API token.")
                return
            }
            BeanProvider.token = botToken
            SpringApplication.run(RoonsApplication::class.java, *args)
        }

        private fun getToken(args: Array<String>): String? {
            val arg = args.firstOrNull { it.startsWith("--bot.token=") } ?: return null
            return arg.split("--bot.token=").getOrNull(1)
        }
    }
}

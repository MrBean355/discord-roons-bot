package com.github.mrbean355.roons

import com.github.mrbean355.roons.discord.RunesDiscordBot
import com.github.mrbean355.roons.dota.GameStateServer

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please pass in only the Discord bot's API token.")
        return
    }
    val discordBot = RunesDiscordBot(args.first())
    discordBot.start()
    GameStateServer(discordBot).start()
}

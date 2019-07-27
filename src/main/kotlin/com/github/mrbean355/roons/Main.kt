package com.github.mrbean355.roons

import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.discord.RunesDiscordBot
import com.github.mrbean355.roons.dota.GameState
import com.github.mrbean355.roons.dota.GameStateMonitor
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Please pass only the Discord bot's token and your auth token as arguments.")
        return
    }
    val bot: DiscordBot = RunesDiscordBot(args[0], args[1])
    bot.start()
    val gameStateMonitor = GameStateMonitor(bot)
    val server = embeddedServer(Netty, port = 12345) {
        install(ContentNegotiation) {
            gson()
        }
        routing {
            post {
                try {
                    val gameState = call.receive<GameState>()
                    if (gameState.isValid() && bot.isAuthorised(gameState.auth!!.token)) {
                        gameStateMonitor.onNewState(gameState)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
    server.start(wait = true)
}

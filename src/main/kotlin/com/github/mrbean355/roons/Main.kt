package com.github.mrbean355.roons

import com.github.mrbean355.roons.discord.RunesDiscordBot
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please pass only the Discord bot's token as an argument.")
        return
    }
    val bot = RunesDiscordBot(args.first())
    Thread { bot.start() }.start()

//    val gameStateMonitor = GameStateMonitor { soundFile ->
//        if (soundFile == SoundFile.ROONS || soundFile == SoundFile.WEED) {
//            bot.playSound(soundFile)
//            true
//        } else {
//            false
//        }
//    }
    val server = embeddedServer(Netty, port = 12345) {
        install(ContentNegotiation) {
            gson()
        }
        routing {
            post {
                try {
                    call.receiveText()
//                    gameStateMonitor.onUpdate(call.receive())
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
    server.start(wait = true)
}

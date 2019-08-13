package com.github.mrbean355.roons

import com.github.mrbean355.roons.di.ClientModule
import com.github.mrbean355.roons.di.DaggerAppComponent
import com.github.mrbean355.roons.discord.UserStore
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please pass in only the Discord bot's API token.")
        return
    }

    val component = DaggerAppComponent.builder()
            .clientModule(ClientModule(args.first()))
            .build()

    val discordBot = component.discordBot()
    discordBot.startAsync()
    embeddedServer(Netty, port = 26382) {
        routing {
            get {
                val token = call.parameters["token"].orEmpty()
                if (UserStore.isTokenValid(token)) {
                    discordBot.playSound(token)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }.start(wait = true)
}

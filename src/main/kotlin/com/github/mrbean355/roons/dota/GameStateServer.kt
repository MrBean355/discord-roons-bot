package com.github.mrbean355.roons.dota

import com.github.mrbean355.roons.discord.SoundEffectPlayer
import com.github.mrbean355.roons.discord.UserStore
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/** Receive game state updates from Dota clients. */
class GameStateServer(soundEffectPlayer: SoundEffectPlayer) {
    private val gameStateMonitor = GameStateMonitor(soundEffectPlayer)
    private val server: ApplicationEngine

    init {
        server = embeddedServer(Netty, port = 26382) {
            install(ContentNegotiation) {
                gson()
            }
            routing {
                post {
                    try {
                        val gameState = call.receive<GameState>()
                        if (UserStore.isTokenValid(gameState.auth?.token.orEmpty()) && gameState.isValid()) {
                            gameStateMonitor.onNewState(gameState)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    fun start() {
        server.start(wait = true)
    }

    fun enableTestMode(token: String) {
        gameStateMonitor.enableTestMode(token)
    }
}
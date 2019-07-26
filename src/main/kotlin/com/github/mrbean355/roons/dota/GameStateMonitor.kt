package com.github.mrbean355.roons.dota

import com.github.mrbean355.roons.discord.DiscordBot
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/** How often (in seconds) the runes spawn. */
private val HOW_OFTEN = TimeUnit.MINUTES.toSeconds(5)
/** How many seconds before the runes spawn to play the sound. */
private val WARNING_PERIOD = TimeUnit.SECONDS.toSeconds(15)

class GameStateMonitor(private val discordBot: DiscordBot) {
    private val matchIds = mutableMapOf<String, String>()
    private val scheduled = mutableMapOf<String, Long>()

    fun onNewState(gameState: GameState) {
        if (shouldPlay(gameState)) {
            discordBot.playSoundRemote(gameState.auth?.token.orEmpty())
        }
    }

    private fun shouldPlay(gameState: GameState): Boolean {
        val token = gameState.auth!!.token
        val currentTime = gameState.map!!.clock_time
        if (currentTime <= 0) {
            return false
        }
        val oldMatch = matchIds[token]
        val currentMatch = gameState.map.matchid
        var nextPlayTime = scheduled[token]
        if (oldMatch != currentMatch || nextPlayTime == null) {
            nextPlayTime = findIteration(currentTime)
            matchIds[token] = currentMatch
            scheduled[token] = nextPlayTime
        }
        if (currentTime >= nextPlayTime) {
            val diff = currentTime - nextPlayTime
            // Ensure we don't schedule another play for the current time:
            scheduled[token] = findIteration(currentTime + 5)
            if (diff <= WARNING_PERIOD) {
                return true
            }
        }
        return false
    }

    private fun findIteration(clockTime: Long): Long {
        val iteration = ceil((clockTime + WARNING_PERIOD) / HOW_OFTEN.toFloat()).toInt()
        val nextPlayTime = iteration * HOW_OFTEN - WARNING_PERIOD
        if (nextPlayTime <= -WARNING_PERIOD) {
            return -WARNING_PERIOD
        }
        return nextPlayTime
    }
}
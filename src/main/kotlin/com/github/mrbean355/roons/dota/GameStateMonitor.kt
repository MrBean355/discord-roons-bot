package com.github.mrbean355.roons.dota

import com.github.mrbean355.roons.discord.SoundEffectPlayer
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/** How often (in seconds) the runes spawn. */
private val HOW_OFTEN = TimeUnit.MINUTES.toSeconds(5)
/** How many seconds before the runes spawn to play the sound. */
private val WARNING_PERIOD = TimeUnit.SECONDS.toSeconds(15)

/** Receives game state updates and plays the ROONS sound when applicable. */
class GameStateMonitor(private val soundEffectPlayer: SoundEffectPlayer) {
    private val matchIds = mutableMapOf<String, String>()
    private val scheduled = mutableMapOf<String, Long>()
    private val testingTokens = mutableSetOf<String>()

    fun onNewState(gameState: GameState) {
        if (shouldPlay(gameState)) {
            soundEffectPlayer.playSound(gameState.auth?.token.orEmpty())
        }
    }

    /** Play the sound as soon as the next game state update is received. */
    fun enableTestMode(token: String) {
        if (token.isNotBlank()) {
            testingTokens += token
        }
    }

    private fun shouldPlay(gameState: GameState): Boolean {
        val token = gameState.auth?.token.orEmpty()
        // Play the sound if the user is trying to test:
        if (token.isNotBlank() && testingTokens.contains(token)) {
            testingTokens.remove(token)
            return true
        }
        val currentTime = gameState.map!!.clock_time
        // Game hasn't started yet (strategy time):
        if (currentTime <= 0) {
            return false
        }
        var nextPlayTime = scheduled[token]
        val previousMatch = matchIds[token]
        val currentMatch = gameState.map.matchid
        // Not scheduled yet or a new match is entered:
        if (nextPlayTime == null || previousMatch != currentMatch) {
            nextPlayTime = findNextPlayTime(currentTime)
            matchIds[token] = currentMatch
            scheduled[token] = nextPlayTime
        }
        // It's time to play the sound:
        if (currentTime >= nextPlayTime) {
            // Ensure we don't schedule another play for the current time:
            scheduled[token] = findNextPlayTime(currentTime + 5)
            // Don't play the sound too late (in case the client stopped sending updates for a while):
            if (currentTime - nextPlayTime <= WARNING_PERIOD) {
                return true
            }
        }
        return false
    }

    private fun findNextPlayTime(currentClockTime: Long): Long {
        val iteration = ceil((currentClockTime + WARNING_PERIOD) / HOW_OFTEN.toFloat()).toInt()
        val nextPlayTime = iteration * HOW_OFTEN - WARNING_PERIOD
        if (nextPlayTime <= -WARNING_PERIOD) {
            return -WARNING_PERIOD
        }
        return nextPlayTime
    }
}
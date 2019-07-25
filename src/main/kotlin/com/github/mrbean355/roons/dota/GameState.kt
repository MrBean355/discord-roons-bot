package com.github.mrbean355.roons.dota

data class GameState(
        val provider: Provider,
        val map: DotaMap?,
        val auth: Auth?) {

    fun isValid(): Boolean {
        return map != null && auth != null && !map.paused
    }
}

data class Provider(
        val name: String,
        val appid: Int,
        val version: Int,
        val timestamp: Long
)

data class DotaMap(
        val name: String,
        val matchid: String,
        val game_time: Long,
        val clock_time: Long,
        val daytime: Boolean,
        val nightstalker_night: Boolean,
        val game_state: String,
        val paused: Boolean,
        val win_team: String,
        val customgamename: String,
        val ward_purchase_cooldown: Int
)

data class Auth(
        val token: String
)
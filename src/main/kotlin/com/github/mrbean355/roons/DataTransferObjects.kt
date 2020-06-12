package com.github.mrbean355.roons

data class CreateIdResponse(val userId: String)

data class AnalyticsRequest(val userId: String, val eventType: String, val eventData: String)

data class PlaySoundRequest(
        val userId: String,
        val token: String,
        val soundFileName: String,
        /** Only present from app 1.10.0 */
        val volume: Int?,
        /** Only present from app 1.10.0 */
        val rate: Int?
)

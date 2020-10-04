package com.github.mrbean355.roons

data class CreateIdResponse(val userId: String)

data class AnalyticsRequest(val userId: String, val eventType: String, val eventData: String)

data class AnalyticsRequestV2(val userId: String, val properties: Map<String, String>)

data class PlaySoundRequest(
        val userId: String,
        val token: String,
        val soundFileName: String,
        /** Only present from app 1.10.0 */
        val volume: Int?,
        /** Only present from app 1.10.0 */
        val rate: Int?
)

data class StatisticsResponse(
        val recentUsers: Int,
        val dailyUsers: Int,
        val properties: Map<String, Map<String, Int>>
)

data class DotaMod(
        val id: String,
        val name: String,
        val description: String,
        val size: Long,
        val hash: String,
        val downloadUrl: String
)
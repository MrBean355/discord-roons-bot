/*
 * Copyright 2023 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons

data class CreateIdResponse(val userId: String)

data class WelcomeMessageResponse(val message: String)

data class AnalyticsRequest(val userId: String, val properties: Map<String, String>)

data class PlaySound(
    val name: String,
    val checksum: String,
)

data class PlaySoundRequest(
    val userId: String,
    val token: String,
    val soundFileName: String,
    /** Only present from app 1.10.0 */
    val volume: Int?,
    /** Only present from app 1.10.0 */
    val rate: Int?
)

data class PlaySoundsRequest(
    val userId: String,
    val token: String,
    val sounds: List<SingleSound>
)

data class SingleSound(
    val soundFileName: String,
    val volume: Int,
    val rate: Int
)

data class DotaModDto(
    val key: String,
    val name: String,
    val description: String,
    val size: Int,
    val hash: String,
    val downloadUrl: String,
    val infoUrl: String
)

data class FeedbackRequest(
    val userId: String,
    val rating: Int,
    val comments: String
)

data class DiscordServerDto(
    val name: String,
    val members: Int,
    val voiceChannel: String?
)

fun DotaMod.asDto(): DotaModDto = DotaModDto(key, name, description, size, hash, downloadUrl, infoUrl)

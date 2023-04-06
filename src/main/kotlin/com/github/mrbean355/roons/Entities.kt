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

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY
import java.util.Date

@Entity
data class AppUser(
    @Id @GeneratedValue(strategy = IDENTITY)
    val id: Int,
    val generatedId: String,
    @Temporal(TemporalType.TIMESTAMP)
    val lastSeen: Date?
)

@Entity
data class DiscordBotSettings(
    @Id @GeneratedValue(strategy = IDENTITY)
    val id: Int,
    val guildId: String,
    var volume: Int,
    var followedUser: String?,
    var lastChannel: String?
)

@Entity
data class DiscordBotUser(
    @Id @GeneratedValue(strategy = IDENTITY)
    val id: Int,
    val discordUserId: String,
    val guildId: String,
    val token: String
)

@Entity
data class AnalyticsProperty(
    @Id @GeneratedValue(strategy = IDENTITY)
    val id: Int,
    @ManyToOne @JoinColumn(referencedColumnName = "id")
    val user: AppUser,
    val property: String,
    @Column(columnDefinition = "TEXT")
    val value: String
)

@Entity
data class Metadata(
    @Id @Column(name = "`key`")
    val key: String,
    val value: String
)

@Entity
data class DotaMod(
    @Id @Column(name = "`key`")
    val key: String,
    val name: String,
    @Column(columnDefinition = "TEXT")
    val description: String,
    val size: Int,
    @Column(columnDefinition = "TEXT")
    val hash: String,
    @Column(columnDefinition = "TEXT")
    val downloadUrl: String,
    @Column(columnDefinition = "TEXT")
    val infoUrl: String
)
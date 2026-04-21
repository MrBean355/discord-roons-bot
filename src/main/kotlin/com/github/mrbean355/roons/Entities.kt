package com.github.mrbean355.roons

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.Instant

@Entity
data class AppUser(
    @Id @GeneratedValue(strategy = IDENTITY)
    val id: Int,
    val generatedId: String,
    val lastSeen: Instant?
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
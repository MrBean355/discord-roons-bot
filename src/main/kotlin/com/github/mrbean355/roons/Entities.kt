package com.github.mrbean355.roons

import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Temporal
import javax.persistence.TemporalType

@Entity
data class AppUser(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val generatedId: String,
        @Temporal(TemporalType.TIMESTAMP) val lastSeen: Date?
)

@Entity
data class DiscordBotSettings(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val guildId: String,
        var volume: Int,
        var followedUser: String?,
        var lastChannel: String?
)

@Entity
data class DiscordBotUser(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val discordUserId: String,
        val guildId: String,
        val token: String
)

@Entity
data class AnalyticsEvent(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val appUserId: String,
        val eventType: String,
        @Column(columnDefinition = "TEXT") val eventData: String,
        val count: Int,
        @Temporal(TemporalType.TIMESTAMP) val lastOccurred: Date?
)

@Entity
data class AnalyticsProperty(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        @ManyToOne @JoinColumn(referencedColumnName = "id") val user: AppUser,
        val property: String,
        @Column(columnDefinition = "TEXT") val value: String
)

@Entity
data class Metadata(
        @Id @Column(name = "`key`") val key: String,
        val value: String
)

@Entity
data class DotaMod(
        @Id @Column(name = "`key`") val key: String,
        val name: String,
        @Column(columnDefinition = "TEXT") val description: String,
        val size: Int,
        @Column(columnDefinition = "TEXT") val hash: String,
        @Column(columnDefinition = "TEXT") val downloadUrl: String,
        @Column(columnDefinition = "TEXT") val infoUrl: String
)
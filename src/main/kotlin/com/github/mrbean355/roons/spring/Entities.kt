package com.github.mrbean355.roons.spring

import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Temporal
import javax.persistence.TemporalType

@Entity
data class AppUser(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val generatedId: String,
        @Temporal(TemporalType.TIMESTAMP) val lastSeen: Date?)

@Entity
data class User(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val userId: String,
        val guildId: String,
        val token: String)

@Entity
data class UserEvent(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val userId: String,
        val eventType: String,
        @Column(columnDefinition = "TEXT") val eventData: String,
        val count: Int)

package com.github.mrbean355.roons.spring

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id

@Entity
data class UserEvent(
        @Id @GeneratedValue(strategy = IDENTITY) val id: Int,
        val userId: String,
        val eventType: String,
        val eventData: String)

package com.github.mrbean355.roons.component

import org.springframework.stereotype.Component
import java.time.Instant

interface Clock {
    val currentTimeMs: Long
    val now: Instant
}

@Component
class DefaultClock : Clock {

    override val currentTimeMs: Long
        get() = System.currentTimeMillis()

    override val now: Instant
        get() = Instant.now()
}
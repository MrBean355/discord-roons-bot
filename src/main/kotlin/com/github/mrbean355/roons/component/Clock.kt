package com.github.mrbean355.roons.component

import org.springframework.stereotype.Component

interface Clock {
    val currentTimeMs: Long
}

@Component
class DefaultClock : Clock {
    override val currentTimeMs: Long
        get() = System.currentTimeMillis()
}
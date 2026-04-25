package com.github.mrbean355.roons

import com.github.mrbean355.roons.component.Clock
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

fun loadTestResource(name: String): String {
    val pathname = Thread.currentThread().contextClassLoader.getResource(name)?.file
    require(pathname != null) { "Resource not found: $name" }
    return File(pathname).readText()
}

fun assertTimeIsRoughlyNow(time: Long?) {
    val diff = System.currentTimeMillis() - (time ?: 0)
    assertTrue(diff < 250, "Expected time $time to be the current time but was off by $diff millis")
}

class TestClock(time: Long) : Clock {
    override val currentTimeMs = time
}
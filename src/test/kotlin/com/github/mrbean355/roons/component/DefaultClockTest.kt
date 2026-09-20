package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.assertTimeIsRoughlyNow
import org.junit.jupiter.api.Test

internal class DefaultClockTest {

    @Test
    internal fun testCurrentTimeMs_ReturnsRoughlyCurrentTime() {
        val ms = DefaultClock().currentTimeMs

        assertTimeIsRoughlyNow(ms)
    }

    @Test
    internal fun testNow_ReturnsRoughlyCurrentInstant() {
        val now = DefaultClock().now

        assertTimeIsRoughlyNow(now.toEpochMilli())
    }
}
package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.discord.DiscordBot
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.JDA
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage
import java.lang.management.RuntimeMXBean

internal class SystemHealthServiceTest {

    @Test
    internal fun testGetSystemHealth() {
        val discordBot = mockk<DiscordBot> {
            every { getGatewayStatus() } returns JDA.Status.CONNECTED
            every { getGatewayPing() } returns 25L
        }
        val runtimeMxBean = mockk<RuntimeMXBean> {
            // 2 days, 3 hours, 45 minutes = (2 * 86400 + 3 * 3600 + 45 * 60) * 1000 ms
            every { uptime } returns 186300000L
        }
        val memoryMxBean = mockk<MemoryMXBean> {
            every { heapMemoryUsage } returns MemoryUsage(
                100L * 1024 * 1024,
                256L * 1024 * 1024,
                512L * 1024 * 1024,
                1024L * 1024 * 1024
            )
        }

        val service = SystemHealthService(discordBot, runtimeMxBean, memoryMxBean)
        val result = service.getSystemHealth()

        assertEquals("2d 3h 45m", result.uptime)
        assertEquals("256 MB / 1024 MB", result.memoryUsage)
        assertEquals("CONNECTED", result.discordStatus)
        assertEquals(25L, result.discordPing)
    }
}

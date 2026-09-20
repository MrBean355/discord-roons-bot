package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.SystemHealthResponse
import com.github.mrbean355.roons.discord.DiscordBot
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.RuntimeMXBean
import java.time.Duration

@Service
class SystemHealthService(
    private val discordBot: DiscordBot,
    private val runtimeMxBean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean(),
    private val memoryMxBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
) {

    fun getSystemHealth(): SystemHealthResponse {
        val uptime = Duration.ofMillis(runtimeMxBean.uptime)
        val heap = memoryMxBean.heapMemoryUsage
        val usedMb = heap.used / (1024 * 1024)
        val maxMb = heap.max / (1024 * 1024)

        return SystemHealthResponse(
            uptime = formatDuration(uptime),
            memoryUsage = "$usedMb MB / $maxMb MB",
            discordStatus = discordBot.getGatewayStatus().name,
            discordPing = discordBot.getGatewayPing()
        )
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        return "${days}d ${hours}h ${minutes}m"
    }
}

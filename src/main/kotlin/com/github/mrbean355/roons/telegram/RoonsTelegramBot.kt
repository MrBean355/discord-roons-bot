package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.discord.DiscordBot
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import java.lang.management.ManagementFactory
import java.time.Duration

const val ENV_TELEGRAM_TOKEN = "TELEGRAM_TOKEN"
const val ENV_TELEGRAM_CHAT = "TELEGRAM_CHAT"

@Component
class RoonsTelegramBot @VisibleForTesting constructor(
    private val discordBot: DiscordBot,
    private val telegramNotifier: TelegramNotifier,
    private val logger: Logger,
    private val adminChatId: String?
) : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    @Autowired
    constructor(
        discordBot: DiscordBot,
        telegramNotifier: TelegramNotifier,
        logger: Logger
    ) : this(discordBot, telegramNotifier, logger, System.getenv(ENV_TELEGRAM_CHAT))

    override fun getBotToken(): String = System.getenv(ENV_TELEGRAM_TOKEN).orEmpty()

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        try {
            if (!update.hasMessage() || !update.message.hasText()) {
                return
            }
            val message = update.message
            val chatId = message.chatId.toString()
            if (adminChatId.isNullOrBlank() || chatId != adminChatId) {
                logger.warn("Ignoring message from non-admin chat ID: $chatId")
                return
            }

            val text = message.text.trim()
            if (text.equals("/status", ignoreCase = true)) {
                handleStatus()
            } else if (text.equals("/help", ignoreCase = true) || text.equals("/start", ignoreCase = true)) {
                handleHelp()
            }
        } catch (e: Exception) {
            logger.error("Error processing Telegram update", e)
        }
    }

    private fun handleStatus() {
        val runtime = ManagementFactory.getRuntimeMXBean()
        val mem = Runtime.getRuntime()
        val uptime = Duration.ofMillis(runtime.uptime)
        val uptimeStr = "${uptime.toDays()}d ${uptime.toHoursPart()}h ${uptime.toMinutesPart()}m"
        val memoryStr = "${(mem.totalMemory() - mem.freeMemory()) / 1024 / 1024} MB / ${mem.maxMemory() / 1024 / 1024} MB"
        val discordStatus = discordBot.getGatewayStatus().name
        val discordPing = "${discordBot.getGatewayPing()} ms"
        val guilds = discordBot.getGuilds()
        val totalGuilds = guilds.size
        val activeVoice = guilds.count { it.audioManager.isConnected }

        telegramNotifier.sendPrivateMessage(
            """
            📊 <b>System Status</b>
            • <b>Uptime</b>: <code>$uptimeStr</code>
            • <b>Memory</b>: <code>$memoryStr</code>
            • <b>Discord Gateway</b>: <code>$discordStatus</code> ($discordPing)
            • <b>Guilds</b>: <code>$totalGuilds</code> ($activeVoice voice active)
            """.trimIndent()
        )
    }

    private fun handleHelp() {
        telegramNotifier.sendPrivateMessage(
            """
            🤖 <b>Roons Admin Bot</b>
            Available commands:
            • <code>/status</code> - Query system health, gateway ping, and active voice channels
            """.trimIndent()
        )
    }
}

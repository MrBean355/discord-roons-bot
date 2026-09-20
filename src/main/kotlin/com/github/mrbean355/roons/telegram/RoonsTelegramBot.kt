package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.discord.DiscordBot
import com.github.mrbean355.roons.service.SystemHealthService
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class RoonsTelegramBot(
    private val discordBot: DiscordBot,
    private val telegramNotifier: TelegramNotifier,
    private val systemHealthService: SystemHealthService,
    private val logger: Logger,
    @Value($$"${TELEGRAM_CHAT}") private val adminChatId: String,
    @Value($$"${TELEGRAM_TOKEN}") private val botToken: String,
) : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    override fun getBotToken(): String = botToken

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        try {
            if (!update.hasMessage() || !update.message.hasText()) {
                return
            }
            val message = update.message
            val chatId = message.chatId.toString()
            if (chatId != adminChatId) {
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
        val health = systemHealthService.getSystemHealth()
        val guilds = discordBot.getGuilds()
        val totalGuilds = guilds.size
        val activeVoice = guilds.count { it.audioManager.isConnected }

        telegramNotifier.sendPrivateMessage(
            """
            📊 <b>System Status</b>
            • <b>Uptime</b>: <code>${health.uptime}</code>
            • <b>Memory</b>: <code>${health.memoryUsage}</code>
            • <b>Discord Gateway</b>: <code>${health.discordStatus}</code> (${health.discordPing} ms)
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

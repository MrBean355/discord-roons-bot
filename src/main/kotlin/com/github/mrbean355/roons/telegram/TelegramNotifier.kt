package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.SendHtmlMessage
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

private const val CHANNEL_ID = "@bulldog_sounds"

@Component
class TelegramNotifier(
    private val bot: TelegramLongPollingBot,
    private val logger: Logger
) {
    private val chatId: String? = System.getenv("TELEGRAM_CHAT")

    fun sendMessage(text: String) {
        if (chatId != null) {
            bot.execute(SendHtmlMessage(chatId, text))
        } else {
            logger.info(text)
        }
    }

    fun sendChannelMessage(text: String) {
        if (chatId != null) {
            bot.execute(SendMessage(CHANNEL_ID, text))
        } else {
            logger.info("$CHANNEL_ID: $text")
        }
    }
}

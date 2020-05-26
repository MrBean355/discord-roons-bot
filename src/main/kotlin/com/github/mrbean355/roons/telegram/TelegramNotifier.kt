package com.github.mrbean355.roons.telegram

import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Component
class TelegramNotifier(
        private val bot: TelegramLongPollingBot,
        private val logger: Logger
) {
    private val chatId = System.getenv("TELEGRAM_CHAT")?.toLong()

    fun sendMessage(text: String) {
        if (chatId != null) {
            bot.execute(SendMessage(chatId, text).enableHtml(true))
        } else {
            logger.info(text)
        }
    }
}

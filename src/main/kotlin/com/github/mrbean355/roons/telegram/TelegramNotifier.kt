package com.github.mrbean355.roons.telegram

import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Component
class TelegramNotifier(private val bot: TelegramLongPollingBot) {
    private val chatId = System.getenv("TELEGRAM_CHAT").toLong()

    fun sendMessage(text: String) {
        bot.execute(SendMessage(chatId, text).enableMarkdown(true))
    }
}


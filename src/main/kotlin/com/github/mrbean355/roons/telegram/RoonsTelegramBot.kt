package com.github.mrbean355.roons.telegram

import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class RoonsTelegramBot : TelegramLongPollingBot(
    System.getenv("TELEGRAM_TOKEN")
) {

    override fun getBotUsername(): String = System.getenv("TELEGRAM_USERNAME")

    override fun onUpdateReceived(update: Update?) {
        // No-op.
    }
}

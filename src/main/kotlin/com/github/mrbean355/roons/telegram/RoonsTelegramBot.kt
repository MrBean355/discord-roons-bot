package com.github.mrbean355.roons.telegram

import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class RoonsTelegramBot : TelegramLongPollingBot() {

    override fun getBotUsername(): String = "RoonsAlertBot"

    override fun getBotToken(): String = System.getenv("TELEGRAM_TOKEN")

    override fun onUpdateReceived(update: Update?) {}
}

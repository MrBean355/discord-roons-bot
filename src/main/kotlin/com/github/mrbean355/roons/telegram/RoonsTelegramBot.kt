package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.SendHtmlMessage
import com.github.mrbean355.roons.getTimeAgo
import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.Calendar.MINUTE

@Component
class RoonsTelegramBot(private val appUserRepository: AppUserRepository) : TelegramLongPollingBot() {
    private val chatId = System.getenv("TELEGRAM_CHAT").orEmpty()

    override fun getBotUsername(): String = System.getenv("TELEGRAM_USERNAME")

    override fun getBotToken(): String = System.getenv("TELEGRAM_TOKEN")

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage() || !update.message.hasText()) {
            return
        }
        if (update.message.text == "/users") {
            usersCommand()
        }
    }

    private fun usersCommand() {
        val activeUsers = appUserRepository.findByLastSeenAfter(getTimeAgo(5, MINUTE)).size
        val message = buildString {
            append("📊 <b>Active Users:</b>\n")
            append("There have been <b>$activeUsers</b> active users in the last 5 minutes.")
        }
        execute(SendHtmlMessage(chatId, message))
    }
}

package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.Calendar
import java.util.Date

@Component
class RoonsTelegramBot(private val appUserRepository: AppUserRepository) : TelegramLongPollingBot() {
    private val chatId = System.getenv("TELEGRAM_CHAT")?.toLong()

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
        val activeUsers = appUserRepository.findByLastSeenAfter(fiveMinutesAgo()).size
        val message = buildString {
            append("📊 <b>Active Users:</b>\n")
            append("There have been <b>$activeUsers</b> active users in the last 5 minutes.")
        }
        execute(SendMessage(chatId, message).enableHtml(true))
    }

    private fun fiveMinutesAgo(): Date {
        return Calendar.getInstance().run {
            add(Calendar.MINUTE, -5)
            time
        }
    }
}

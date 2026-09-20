package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.SendHtmlMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.generics.TelegramClient

private const val CHANNEL_ID = "@bulldog_sounds"

@Component
class TelegramNotifier(
    private val bot: TelegramClient,
    @Value($$"${TELEGRAM_CHAT}") private val chatId: String,
) {

    fun sendPrivateMessage(text: String) {
        bot.execute(SendHtmlMessage(chatId, text))
    }

    fun sendChannelMessage(text: String) {
        bot.execute(SendHtmlMessage(CHANNEL_ID, text))
    }
}

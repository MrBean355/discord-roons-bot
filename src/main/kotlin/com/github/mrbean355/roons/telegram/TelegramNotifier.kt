package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.SendHtmlMessage
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot

private const val CHANNEL_ID = "@bulldog_sounds"

@Component
class TelegramNotifier @VisibleForTesting constructor(
    private val bot: TelegramLongPollingBot,
    private val logger: Logger,
    private val chatId: String?
) {

    @Autowired
    constructor(bot: TelegramLongPollingBot, logger: Logger) : this(bot, logger, System.getenv("TELEGRAM_CHAT"))

    fun sendPrivateMessage(text: String) {
        if (chatId != null) {
            bot.execute(SendHtmlMessage(chatId, text))
        } else {
            logger.info(text)
        }
    }

    fun sendChannelMessage(text: String) {
        if (chatId != null) {
            bot.execute(SendHtmlMessage(CHANNEL_ID, text))
        } else {
            logger.info("$CHANNEL_ID: $text")
        }
    }
}

package com.github.mrbean355.roons.telegram

import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

const val ENV_TELEGRAM_TOKEN = "TELEGRAM_TOKEN"
const val ENV_TELEGRAM_CHAT = "TELEGRAM_CHAT"

@Component
class RoonsTelegramBot : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    override fun getBotToken(): String = System.getenv(ENV_TELEGRAM_TOKEN)

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    override fun consume(update: Update) {
        // No-op.
    }
}

package com.github.mrbean355.roons

import org.telegram.telegrambots.meta.api.methods.ParseMode.HTML
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.util.Calendar
import java.util.Date

/**
 * @return the current `Date` that has the [amount] of `Calendar` [field] subtracted.
 */
fun getTimeAgo(amount: Int, field: Int): Date {
    return Calendar.getInstance().run {
        add(field, -amount)
        time
    }
}

@Suppress("FunctionName")
fun SendHtmlMessage(chatId: String, text: String): SendMessage = SendMessage.builder()
    .chatId(chatId)
    .text(text)
    .parseMode(HTML)
    .build()
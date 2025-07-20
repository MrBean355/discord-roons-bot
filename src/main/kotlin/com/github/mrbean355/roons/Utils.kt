package com.github.mrbean355.roons

import org.telegram.telegrambots.meta.api.methods.ParseMode.HTML
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Suppress("FunctionName")
fun SendHtmlMessage(chatId: String, text: String): SendMessage = SendMessage.builder()
    .chatId(chatId)
    .text(text)
    .parseMode(HTML)
    .build()

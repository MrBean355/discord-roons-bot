package com.github.mrbean355.roons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.ParseMode

internal class UtilsKtTest {

    @Test
    internal fun testSendHtmlMessage() {
        val message = SendHtmlMessage("12345", "Hello world")

        assertEquals("12345", message.chatId)
        assertEquals("Hello world", message.text)
        assertEquals(ParseMode.HTML, message.parseMode)
    }
}
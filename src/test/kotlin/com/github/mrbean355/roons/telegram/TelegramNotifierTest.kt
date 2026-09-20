package com.github.mrbean355.roons.telegram

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient

internal class TelegramNotifierTest {
    @MockK(relaxed = true)
    private lateinit var bot: TelegramClient

    private lateinit var notifier: TelegramNotifier

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        notifier = TelegramNotifier(bot, "12345")
    }

    @Test
    internal fun testSendPrivateMessage_SendsTelegramMessage() {
        notifier.sendPrivateMessage("allo")

        val slot = slot<SendMessage>()
        verify { bot.execute(capture(slot)) }
        with(slot.captured) {
            assertEquals("12345", chatId)
            assertEquals("allo", text)
            assertEquals(ParseMode.HTML, parseMode)
        }
    }

    @Test
    internal fun testSendChannelMessage_SendsTelegramMessage() {
        notifier.sendChannelMessage("allo")

        val slot = slot<SendMessage>()
        verify { bot.execute(capture(slot)) }
        with(slot.captured) {
            assertEquals("@bulldog_sounds", chatId)
            assertEquals("allo", text)
            assertEquals(ParseMode.HTML, parseMode)
        }
    }
}
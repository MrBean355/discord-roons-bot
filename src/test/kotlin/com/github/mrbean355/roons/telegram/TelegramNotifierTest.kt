/*
 * Copyright 2021 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mrbean355.roons.telegram

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

internal class TelegramNotifierTest {
    @MockK(relaxed = true)
    private lateinit var bot: TelegramLongPollingBot

    @MockK(relaxed = true)
    private lateinit var logger: Logger

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    internal fun testSendPrivateMessage_NoChatId_LogsMessage() {
        val notifier = TelegramNotifier(bot, logger, null)

        notifier.sendPrivateMessage("allo")

        verify(inverse = true) { bot.execute(any<SendMessage>()) }
        verify { logger.info("allo") }
    }

    @Test
    internal fun testSendPrivateMessage_HasChatId_SendsTelegramMessage() {
        val notifier = TelegramNotifier(bot, logger, "12345")

        notifier.sendPrivateMessage("allo")

        verify(inverse = true) { logger.info("allo") }
        val slot = slot<SendMessage>()
        verify { bot.execute(capture(slot)) }
        with(slot.captured) {
            assertEquals("12345", chatId)
            assertEquals("allo", text)
            assertEquals(ParseMode.HTML, parseMode)
        }
    }

    @Test
    internal fun testSendChannelMessage_NoChatId_LogsMessage() {
        val notifier = TelegramNotifier(bot, logger, null)

        notifier.sendChannelMessage("allo")

        verify(inverse = true) { bot.execute(any<SendMessage>()) }
        verify { logger.info("@bulldog_sounds: allo") }
    }

    @Test
    internal fun testSendChannelMessage_HasChatId_SendsTelegramMessage() {
        val notifier = TelegramNotifier(bot, logger, "12345")

        notifier.sendChannelMessage("allo")

        verify(inverse = true) { logger.info("allo") }
        val slot = slot<SendMessage>()
        verify { bot.execute(capture(slot)) }
        with(slot.captured) {
            assertEquals("@bulldog_sounds", chatId)
            assertEquals("allo", text)
            assertEquals(ParseMode.HTML, parseMode)
        }
    }
}
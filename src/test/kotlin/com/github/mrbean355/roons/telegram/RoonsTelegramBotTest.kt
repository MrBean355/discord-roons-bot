package com.github.mrbean355.roons.telegram

import com.github.mrbean355.roons.discord.DiscordBot
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.dv8tion.jda.api.JDA
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.Update

internal class RoonsTelegramBotTest {

    @MockK(relaxed = true)
    private lateinit var discordBot: DiscordBot

    @MockK(relaxed = true)
    private lateinit var telegramNotifier: TelegramNotifier

    @MockK(relaxed = true)
    private lateinit var logger: Logger

    private val adminChatId = "123456"

    private lateinit var bot: RoonsTelegramBot

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        bot = RoonsTelegramBot(discordBot, telegramNotifier, logger, adminChatId)
    }

    @Test
    internal fun testConsume_StatusFromAdmin_SendsStatusReport() {
        every { discordBot.getGatewayStatus() } returns JDA.Status.CONNECTED
        every { discordBot.getGatewayPing() } returns 35L
        every { discordBot.getGuilds() } returns emptyList()

        val update = mockk<Update>()
        val message = mockk<Message>()
        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.hasText() } returns true
        every { message.chatId } returns 123456L
        every { message.text } returns "/status"

        bot.consume(update)

        val slot = slot<String>()
        verify { telegramNotifier.sendPrivateMessage(capture(slot)) }
        assertTrue(slot.captured.contains("System Status"))
        assertTrue(slot.captured.contains("CONNECTED"))
        assertTrue(slot.captured.contains("35 ms"))
    }

    @Test
    internal fun testConsume_NonAdminUser_IgnoresMessage() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.hasText() } returns true
        every { message.chatId } returns 999999L
        every { message.text } returns "/status"

        bot.consume(update)

        verify(inverse = true) { telegramNotifier.sendPrivateMessage(any()) }
        verify { logger.warn(match { it.contains("non-admin") }) }
    }

    @Test
    internal fun testConsume_HelpFromAdmin_SendsHelpMessage() {
        val update = mockk<Update>()
        val message = mockk<Message>()
        every { update.hasMessage() } returns true
        every { update.message } returns message
        every { message.hasText() } returns true
        every { message.chatId } returns 123456L
        every { message.text } returns "/help"

        bot.consume(update)

        val slot = slot<String>()
        verify { telegramNotifier.sendPrivateMessage(capture(slot)) }
        assertTrue(slot.captured.contains("Roons Admin Bot"))
        assertTrue(slot.captured.contains("/status"))
    }

    @Test
    internal fun testConsume_NoTextMessage_DoesNothing() {
        val update = mockk<Update>()
        every { update.hasMessage() } returns false

        bot.consume(update)

        verify(inverse = true) { telegramNotifier.sendPrivateMessage(any()) }
    }
}

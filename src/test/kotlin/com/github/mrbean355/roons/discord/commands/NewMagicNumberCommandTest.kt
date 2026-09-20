package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.service.DiscordBotService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class NewMagicNumberCommandTest {

    @MockK(relaxed = true)
    private lateinit var discordBotService: DiscordBotService

    @MockK(relaxed = true)
    private lateinit var event: SlashCommandInteractionEvent

    @MockK(relaxed = true)
    private lateinit var member: Member

    @MockK(relaxed = true)
    private lateinit var guild: Guild

    private val replySlot = slot<String>()
    private val replyAction = mockk<ReplyCallbackAction>(relaxed = true)
    private lateinit var command: NewMagicNumberCommand

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        command = NewMagicNumberCommand(discordBotService)

        every { replyAction.setEphemeral(any()) } returns replyAction
        every { event.reply(capture(replySlot)) } returns replyAction
        every { event.member } returns member
        every { member.guild } returns guild
        every { member.id } returns "user_1"
        every { guild.id } returns "guild_1"
    }

    @Test
    internal fun testHandleCommand_CallsServiceAndRepliesEphemerallyWithNewToken() {
        every { discordBotService.generateNewUserToken("user_1", "guild_1") } returns "token_67890"

        command.handleCommand(event)

        assertTrue(replySlot.captured.contains("token_67890"))
        verify { replyAction.setEphemeral(true) }
        verify { replyAction.queue() }
    }
}

package com.github.mrbean355.roons.discord.commands

import com.github.mrbean355.roons.DiscordBotSettings
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FollowCommandTest {

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
    private lateinit var command: FollowCommand

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        command = FollowCommand(discordBotService)

        every { replyAction.setEphemeral(any()) } returns replyAction
        every { event.reply(capture(replySlot)) } returns replyAction

        every { event.member } returns member
        every { member.guild } returns guild
        every { member.id } returns "user_1"
        every { member.asMention } returns "<@user_1>"
        every { guild.id } returns "guild_1"
        every { guild.getMemberById(any<String>()) } returns null
    }

    @Test
    internal fun testHandleCommand_AlreadyFollowingUser_RepliesEphemeral() {
        every { discordBotService.loadSettings("guild_1") } returns DiscordBotSettings(1, "guild_1", 100, followedUser = "user_1", lastChannel = null)

        command.handleCommand(event)

        assertEquals("I'm already following you.", replySlot.captured)
        verify { replyAction.setEphemeral(true) }
        verify { replyAction.queue() }
        verify(inverse = true) { discordBotService.saveSettings(any()) }
    }

    @Test
    internal fun testHandleCommand_NewFollow_NoPreviousFollower_SetsFollowedUserAndReplies() {
        every { discordBotService.loadSettings("guild_1") } returns DiscordBotSettings(1, "guild_1", 100, followedUser = null, lastChannel = null)

        command.handleCommand(event)

        assertEquals("I'm now following <@user_1>.", replySlot.captured)
        verify { discordBotService.saveSettings(match { it.followedUser == "user_1" }) }
        verify { replyAction.queue() }
    }

    @Test
    internal fun testHandleCommand_PreviousFollowedUserCached_MentionsBothUsers() {
        every { discordBotService.loadSettings("guild_1") } returns DiscordBotSettings(1, "guild_1", 100, followedUser = "user_2", lastChannel = null)
        val previousMember = mockk<Member>(relaxed = true)
        every { previousMember.asMention } returns "<@user_2>"
        every { guild.getMemberById("user_2") } returns previousMember

        command.handleCommand(event)

        assertEquals("I'm now following <@user_1> instead of <@user_2>.", replySlot.captured)
        verify { discordBotService.saveSettings(match { it.followedUser == "user_1" }) }
        verify { replyAction.queue() }
    }

    @Test
    internal fun testHandleCommand_PreviousFollowedUserNotFoundOrThrows_MentionsOnlyNewUser() {
        every { discordBotService.loadSettings("guild_1") } returns DiscordBotSettings(1, "guild_1", 100, followedUser = "user_2", lastChannel = null)
        every { guild.getMemberById("user_2") } returns null
        every { guild.retrieveMemberById("user_2").complete() } throws RuntimeException("Unknown Member")

        command.handleCommand(event)

        assertEquals("I'm now following <@user_1>.", replySlot.captured)
        verify { discordBotService.saveSettings(match { it.followedUser == "user_1" }) }
        verify { replyAction.queue() }
    }
}

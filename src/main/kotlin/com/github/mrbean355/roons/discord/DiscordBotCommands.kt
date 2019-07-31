package com.github.mrbean355.roons.discord

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

/** A command that can be entered by a Discord user. */
sealed class BotCommand(protected val discordBot: DiscordBot) {
    /** Text the user types to execute the command. */
    abstract val input: String

    /** Execute the command. */
    abstract fun execute(event: MessageCreateEvent): Mono<Void>
}

/** Send a link to the GitHub repo. */
class HelpCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override val input = "help"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.message.channel
                .flatMap { it.createMessage("Setup instructions and available commands can be found here: https://github.com/MrBean355/discord-roons-bot :tools:") }
                .then()
    }
}

/** Join the user's current voice channel. */
class JoinCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override val input = "roons"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
                .flatMap { it.voiceState }
                .flatMap { it.channel }
                .zipWith(event.message.channel)
                .flatMap { tuple ->
                    tuple.t2.createMessage("Joining voice channel `${tuple.t1.name}` :microphone2:")
                            .map { tuple.t1 }
                }
                .zipWith(event.guild)
                .flatMap { tuple ->
                    val playerManager = discordBot.playerManager(tuple.t2)
                    tuple.t1.join { spec ->
                        spec.setProvider(playerManager.audioProvider)
                    }.zipWith(Mono.just(playerManager))
                }
                .doOnNext { it.t2.voiceConnection = it.t1 }
                .then()
    }
}

/** Leave the current voice channel. */
class LeaveCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override val input = "seeya"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.guild
                .map { discordBot.playerManager(it) }
                .flatMap {
                    val connection = it.voiceConnection
                    if (connection != null) {
                        connection.disconnect()
                        it.voiceConnection = null
                        Mono.just(Any())
                    } else {
                        Mono.empty<Any>()
                    }
                }
                .then(event.message.channel)
                .flatMap {
                    it.createMessage("Goodbye :wave:")
                }
                .then()
    }
}

/** Send a private message with the user's token. */
class MagicCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override val input = "magic"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
                .flatMap { it.privateChannel }
                .zipWith(event.guild)
                .flatMap { tuple ->
                    tuple.t1.createMessage("""
                    Your magic number is: `${UserStore.getOrCreate(event.member.get(), tuple.t2)}` :sparkles:
                    Don't give this to other people!
                    Use `!help` for setup instructions
                """.trimIndent())
                }
                .then()
    }
}

/** Play a test sound on the next game state update. */
class TestCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override val input = "test"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
                .zipWith(event.guild)
                .doOnNext { discordBot.enableTest(UserStore.getOrCreate(it.t1, it.t2)) }
                .then(event.message.channel)
                .flatMap { it.createMessage("Test mode engaged! Enter hero demo mode to test your setup :spy:") }
                .then()
    }
}

/** Get or set the guild's audio player's volume. */
class VolumeCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override val input = "volume"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.message.content)
                .flatMap {
                    val s = it.split(' ')
                    when {
                        s.size == 1 -> showCurrentVolume(event.guild, event.message.channel)
                        s.size == 2 -> setCurrentVolume(s[1], event.guild, event.message.channel)
                        else -> Mono.empty<Void>()
                    }
                }
                .then()
    }

    private fun showCurrentVolume(guild: Mono<Guild>, channel: Mono<MessageChannel>): Mono<Void> {
        return guild.zipWith(channel)
                .flatMap { it.t2.createMessage("Current volume is `${discordBot.getVolume(it.t1)}%` :headphones:") }
                .then()
    }

    private fun setCurrentVolume(volumeStr: String, guild: Mono<Guild>, channel: Mono<MessageChannel>): Mono<Void> {
        return Mono.just(volumeStr)
                .filter { it.matches(Regex("-?\\d+")) }
                .map { it.toInt().coerceAtLeast(0).coerceAtMost(100) }
                .flatMap { volume ->
                    channel.flatMap { it.createMessage("Setting volume to `$volume%` :sound:") }
                            .map { volume }
                }
                .zipWith(guild)
                .doOnNext { discordBot.setVolume(it.t2, it.t1) }
                .then()
    }
}

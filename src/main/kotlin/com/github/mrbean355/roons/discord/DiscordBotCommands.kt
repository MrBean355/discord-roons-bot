package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.COMMAND_PREFIX
import com.github.mrbean355.roons.HELP_URL
import com.github.mrbean355.roons.VOLUME_MAX
import com.github.mrbean355.roons.VOLUME_MIN
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

/** @return all available commands. */
fun allCommands(callbacks: CommandCallbacks): Set<BotCommand> {
    return setOf(HelpCommand(callbacks), JoinCommand(callbacks), LeaveCommand(callbacks), MagicCommand(callbacks), TestCommand(callbacks), VolumeCommand(callbacks))
}

interface CommandCallbacks {
    fun getPlayerManager(guild: Guild): GuildPlayerManager
    fun enableTestMode(token: String)
}

/** A command that can be entered by a Discord user. */
sealed class BotCommand(protected val callbacks: CommandCallbacks) {
    /** Text the user must enter to execute the command (excluding the [COMMAND_PREFIX]). */
    abstract val input: String

    /** Execute the command. */
    abstract fun execute(event: MessageCreateEvent): Mono<Void>
}

/** Send a link to the GitHub repo. */
class HelpCommand(callbacks: CommandCallbacks) : BotCommand(callbacks) {
    override val input = "help"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.message.channel
                .flatMap { it.createMessage("Setup instructions and available commands can be found here: $HELP_URL :tools:") }
                .then()
    }
}

/** Join the user's current voice channel. */
class JoinCommand(callbacks: CommandCallbacks) : BotCommand(callbacks) {
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
                    val playerManager = callbacks.getPlayerManager(tuple.t2)
                    tuple.t1.join { spec ->
                        spec.setProvider(playerManager.audioProvider)
                    }.zipWith(Mono.just(playerManager))
                }
                .doOnNext { it.t2.onVoiceConnected(it.t1) }
                .then()
    }
}

/** Leave the current voice channel. */
class LeaveCommand(callbacks: CommandCallbacks) : BotCommand(callbacks) {
    override val input = "seeya"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.guild
                .map { callbacks.getPlayerManager(it) }
                .flatMap { it.tryDisconnect() }
                .then(event.message.channel)
                .flatMap {
                    it.createMessage("Goodbye :wave:")
                }
                .then()
    }
}

/** Send a private message with the user's token. */
class MagicCommand(callbacks: CommandCallbacks) : BotCommand(callbacks) {
    override val input = "magic"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
                .flatMap { it.privateChannel }
                .zipWith(event.guild)
                .flatMap { tuple ->
                    tuple.t1.createMessage("""
                    Your magic number is: `${UserStore.getOrCreate(event.member.get(), tuple.t2)}` :sparkles:
                    Don't give this to other people!
                    Use `${COMMAND_PREFIX}help` for setup instructions
                """.trimIndent())
                }
                .then()
    }
}

/** Play a test sound on the next game state update. */
class TestCommand(callbacks: CommandCallbacks) : BotCommand(callbacks) {
    override val input = "test"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.member)
                .zipWith(event.guild)
                .doOnNext { callbacks.enableTestMode(UserStore.getOrCreate(it.t1, it.t2)) }
                .then(event.message.channel)
                .flatMap {
                    it.createMessage("""
                    Test mode engaged! :rocket:
                    Enter hero demo mode to test the bot.
                    You should hear the ROONS sound as soon as the game clock ticks. 
                    """.trimIndent())
                }
                .then()
    }
}

/** Get or set the guild's audio player's volume. */
class VolumeCommand(callbacks: CommandCallbacks) : BotCommand(callbacks) {
    override val input = "volume"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.message.content)
                .flatMap {
                    val s = it.split(' ')
                    when {
                        s.size == 1 -> showCurrentVolume(event.guild, event.message.channel)
                        s.size == 2 -> setCurrentVolume(s[1], event.guild, event.message.channel)
                        else -> Mono.empty()
                    }
                }
                .then()
    }

    private fun showCurrentVolume(guild: Mono<Guild>, channel: Mono<MessageChannel>): Mono<Void> {
        return guild.zipWith(channel)
                .map { callbacks.getPlayerManager(it.t1) to it.t2 }
                .flatMap { it.second.createMessage("Current volume is `${it.first.getVolume()}%` :headphones:") }
                .then()
    }

    private fun setCurrentVolume(volumeStr: String, guild: Mono<Guild>, channel: Mono<MessageChannel>): Mono<Void> {
        return Mono.just(volumeStr)
                .filter { it.matches(Regex("-?\\d+")) }
                .map { it.toInt().coerceAtLeast(VOLUME_MIN).coerceAtMost(VOLUME_MAX) }
                .flatMap { volume ->
                    channel.flatMap { it.createMessage("Setting volume to `$volume%` :sound:") }
                            .map { volume }
                }
                .zipWith(guild)
                .map { callbacks.getPlayerManager(it.t2) to it.t1 }
                .doOnNext { it.first.setVolume(it.second) }
                .then()
    }
}

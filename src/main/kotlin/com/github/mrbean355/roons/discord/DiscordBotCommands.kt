package com.github.mrbean355.roons.discord

import com.github.mrbean355.roons.COMMAND_PREFIX
import com.github.mrbean355.roons.HELP_URL
import com.github.mrbean355.roons.VOLUME_MAX
import com.github.mrbean355.roons.VOLUME_MIN
import com.github.mrbean355.roons.discord.audio.GuildPlayerManagerProvider
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.function.Tuples

/** A command that can be entered by a Discord user. */
sealed class BotCommand {
    /** Text the user must enter to execute the command (excluding the [COMMAND_PREFIX]). */
    abstract val input: String

    /** Execute the command. */
    abstract fun execute(event: MessageCreateEvent): Mono<Void>
}

/** Send a link to the GitHub repo. */
@Component
class HelpCommand : BotCommand() {
    override val input = "help"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.message.channel
                .flatMap { it.createMessage("Setup instructions and available commands can be found here: $HELP_URL :tools:") }
                .then()
    }
}

/** Join the user's current voice channel. */
@Component
class JoinCommand constructor(private val provider: GuildPlayerManagerProvider) : BotCommand() {
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
                .map { Tuples.of(it.t1, provider.get(it.t2)) }
                .flatMap { tuple ->
                    tuple.t2.tryDisconnect()
                            .then(tuple.t1.join { spec ->
                                spec.setProvider(tuple.t2.audioProvider)
                            }).zipWith(Mono.just(tuple.t2))
                }
                .doOnNext { it.t2.onVoiceConnected(it.t1) }
                .then()
    }
}

/** Leave the current voice channel. */
@Component
class LeaveCommand constructor(private val provider: GuildPlayerManagerProvider) : BotCommand() {
    override val input = "seeya"

    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.guild
                .map { provider.get(it) }
                .flatMap { it.tryDisconnect() }
                .then(event.message.channel)
                .flatMap {
                    it.createMessage("Goodbye :wave:")
                }
                .then()
    }
}

/** Send a private message with the user's token. */
@Component
class MagicCommand : BotCommand() {
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
                .then(event.message.addReaction(ReactionEmoji.unicode("✅")))
                .then()
    }
}

/** Get or set the guild's audio player's volume. */
@Component
class VolumeCommand constructor(private val provider: GuildPlayerManagerProvider) : BotCommand() {
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
                .map { provider.get(it.t1) to it.t2 }
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
                .map { provider.get(it.t2) to it.t1 }
                .doOnNext { it.first.setVolume(it.second) }
                .then()
    }
}

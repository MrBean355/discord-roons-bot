package com.github.mrbean355.roons.discord

import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono

sealed class BotCommand(protected val discordBot: DiscordBot) {
    abstract fun execute(event: MessageCreateEvent): Mono<Void>
}

class HelpCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.message.channel
                .flatMap {
                    it.createMessage(":information_source: Available Commands :information_source:\n" +
                            "`!halp` - This message\n" +
                            "`!wtff` - Instructions on how to set up your PC\n" +
                            "`!roons` - Join your current voice channel\n" +
                            "`!rekt` - Leave the current voice channel\n" +
                            "`!test` - Play a test sound")
                }
                .then()
    }
}

class JoinCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
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
                .flatMap { discordBot.joinVoiceChannel(it.t2, it.t1) }
                .then()
    }
}

class LeaveCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.guild
                .map { discordBot.leaveVoiceChannel(it) }
                .zipWith(event.message.channel)
                .flatMap {
                    if (it.t1) {
                        it.t2.createMessage("Goodbye :wave:")
                    } else {
                        it.t2.createMessage("Not connected to a voice channel :shrug:")
                    }
                }
                .then()
    }
}

class TestCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return event.message.channel
                .then(event.guild)
                .map { discordBot.playSound(it) }
                .zipWith(event.message.channel)
                .flatMap {
                    if (it.t1) {
                        it.t2.createMessage("Playing test sound :headphones:")
                    } else {
                        it.t2.createMessage("Not connected to a voice channel :shrug:")
                    }
                }
                .then()
    }
}

class VolumeCommand(discordBot: DiscordBot) : BotCommand(discordBot) {
    override fun execute(event: MessageCreateEvent): Mono<Void> {
        return Mono.justOrEmpty(event.message.content)
                .map { it.split(' ') }
                .filter { it.size == 2 }
                .flatMap { Mono.just(it[1]) }
                .filter { it.matches(Regex("-?\\d+")) }
                .flatMap { Mono.just(it.toInt().coerceAtLeast(0).coerceAtMost(100)) }
                .zipWith(event.message.channel)
                .flatMap { tuple ->
                    tuple.t2.createMessage("Setting volume to `${tuple.t1}%` :sound: ")
                            .map { tuple.t1 }
                }
                .zipWith(event.guild)
                .doOnNext {
                    discordBot.setVolume(it.t2, it.t1)
                }
                .then()
    }
}

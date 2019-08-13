package com.github.mrbean355.roons.di

import com.github.mrbean355.roons.discord.*
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
interface CommandModule {

    @Binds
    @IntoSet
    fun bindHelpCommand(impl: HelpCommand): BotCommand

    @Binds
    @IntoSet
    fun bindJoinCommand(impl: JoinCommand): BotCommand

    @Binds
    @IntoSet
    fun bindLeaveCommand(impl: LeaveCommand): BotCommand

    @Binds
    @IntoSet
    fun bindMagicCommand(impl: MagicCommand): BotCommand

    @Binds
    @IntoSet
    fun bindVolumeCommand(impl: VolumeCommand): BotCommand
}
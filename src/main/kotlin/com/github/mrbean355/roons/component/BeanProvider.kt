/*
 * Copyright 2023 Michael Johnston
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

package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.discord.DiscordEventHandler
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Scope
import org.springframework.context.support.GenericApplicationContext
import org.springframework.stereotype.Component

/** Environment variable for the Discord API token. */
private const val ENV_DISCORD_TOKEN = "DISCORD_API_TOKEN"

/** Bean qualifier for injecting the `token` string. */
const val DISCORD_TOKEN = "discord-token"

@Component
object BeanProvider {

    @Bean
    @Qualifier(DISCORD_TOKEN)
    fun discordToken(applicationContext: GenericApplicationContext): String {
        return applicationContext.environment.systemEnvironment[ENV_DISCORD_TOKEN] as String
    }

    @Bean
    fun jda(
        @Qualifier(DISCORD_TOKEN) token: String,
        discordEventHandler: DiscordEventHandler
    ): JDA = JDABuilder.createDefault(token)
        .addEventListeners(discordEventHandler)
        .build()

    @Bean
    @Scope("prototype")
    fun logger(injectionPoint: InjectionPoint): Logger {
        val clazz = injectionPoint.methodParameter?.containingClass
            ?: injectionPoint.field?.declaringClass
        return LoggerFactory.getLogger(clazz)
    }
}

package com.github.mrbean355.roons.component

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Scope
import org.springframework.context.support.GenericApplicationContext
import org.springframework.stereotype.Component
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

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
    @Scope("prototype")
    fun logger(injectionPoint: InjectionPoint): Logger {
        val clazz = injectionPoint.methodParameter?.containingClass
                ?: injectionPoint.field?.declaringClass
        return LoggerFactory.getLogger(clazz)
    }

    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder().region(Region.US_EAST_2).build()
    }
}

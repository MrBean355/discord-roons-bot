package com.github.mrbean355.roons.component

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

/** Bean qualifier for injecting the `token` string. */
const val TOKEN = "token"

@Component
object BeanProvider {
    private var token = ""

    fun setToken(token: String) {
        this.token = token
    }

    @Bean
    @Qualifier(TOKEN)
    fun token() = token

    @Bean
    fun logger(injectionPoint: InjectionPoint): Logger {
        val clazz = injectionPoint.methodParameter?.containingClass
                ?: injectionPoint.field?.declaringClass
        return LoggerFactory.getLogger(clazz)
    }
}

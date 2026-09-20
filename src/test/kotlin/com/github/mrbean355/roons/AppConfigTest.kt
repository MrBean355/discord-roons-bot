package com.github.mrbean355.roons

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.InjectionPoint
import org.springframework.web.servlet.config.annotation.ViewControllerRegistration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry

internal class AppConfigTest {

    private val appConfig = AppConfig()

    @Test
    internal fun testLogger_ContainingClass() {
        val injectionPoint = mockk<InjectionPoint> {
            every { methodParameter } returns mockk {
                every { containingClass } returns ContainingClass::class.java
            }
        }

        val result = appConfig.logger(injectionPoint)

        assertEquals(ContainingClass::class.java.name, result.name)
    }

    @Test
    internal fun testLogger_DeclaringClass() {
        val injectionPoint = mockk<InjectionPoint> {
            every { methodParameter } returns null
            every { field } returns mockk {
                every { declaringClass } returns DeclaringClass::class.java
            }
        }

        val result = appConfig.logger(injectionPoint)

        assertEquals(DeclaringClass::class.java.name, result.name)
    }

    @Test
    internal fun testAddViewControllers_RegistersTermsAndPrivacy() {
        val termsRegistration = mockk<ViewControllerRegistration>(relaxed = true)
        val privacyRegistration = mockk<ViewControllerRegistration>(relaxed = true)
        val registry = mockk<ViewControllerRegistry> {
            every { addViewController("/terms") } returns termsRegistration
            every { addViewController("/privacy") } returns privacyRegistration
        }

        appConfig.addViewControllers(registry)

        verify {
            termsRegistration.setViewName("forward:/terms.html")
            privacyRegistration.setViewName("forward:/privacy.html")
        }
    }
}

private class ContainingClass
private class DeclaringClass

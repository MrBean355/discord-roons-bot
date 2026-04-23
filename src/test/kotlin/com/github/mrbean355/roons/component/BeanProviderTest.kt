package com.github.mrbean355.roons.component

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.InjectionPoint
import org.springframework.context.support.GenericApplicationContext

internal class BeanProviderTest {

    @Test
    internal fun testDiscordToken() {
        val context = mockk<GenericApplicationContext> {
            every { environment } returns mockk {
                every { systemEnvironment } returns mapOf("DISCORD_API_TOKEN" to "abc123")
            }
        }

        val result = BeanProvider.discordToken(context)

        assertEquals("abc123", result)
    }

    @Test
    internal fun testLogger_ContainingClass() {
        val injectionPoint = mockk<InjectionPoint> {
            every { methodParameter } returns mockk {
                every { containingClass } returns ContainingClass::class.java
            }
        }

        val result = BeanProvider.logger(injectionPoint)

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

        val result = BeanProvider.logger(injectionPoint)

        assertEquals(DeclaringClass::class.java.name, result.name)
    }
}

private class ContainingClass
private class DeclaringClass
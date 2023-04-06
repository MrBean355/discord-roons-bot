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
/*
 * Copyright 2021 Michael Johnston
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

package com.github.mrbean355.roons

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.ParseMode
import java.util.Optional

internal class UtilsKtTest {

    @Test
    internal fun testSendHtmlMessage() {
        val message = SendHtmlMessage("12345", "Hello world")

        assertEquals("12345", message.chatId)
        assertEquals("Hello world", message.text)
        assertEquals(ParseMode.HTML, message.parseMode)
    }

    @Test
    internal fun testOptionalOrNull_NullValue_ReturnsNull() {
        val result = Optional.ofNullable<String>(null).orNull()

        assertNull(result)
    }

    @Test
    internal fun testOptionalOrNull_NonNullValue_ReturnsValue() {
        val result = Optional.ofNullable<String>("abc").orNull()

        assertEquals("abc", result)
    }
}
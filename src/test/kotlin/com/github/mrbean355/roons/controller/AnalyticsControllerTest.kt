/*
 * Copyright 2022 Michael Johnston
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

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.AnalyticsRequest
import com.github.mrbean355.roons.component.Analytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
internal class AnalyticsControllerTest {
    @MockK
    private lateinit var analytics: Analytics
    private lateinit var controller: AnalyticsController

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        controller = AnalyticsController(analytics)
    }

    @Test
    internal fun testLogProperties_DelegatesToAnalytics(
        @MockK properties: Map<String, String>
    ) {
        every { analytics.logProperties(any(), any()) } returns true

        controller.logProperties(AnalyticsRequest("12345", properties))

        verify { analytics.logProperties("12345", properties) }
    }

    @Test
    internal fun testLogProperties_AnalyticsReturnsTrue_ReturnsOkResult() {
        every { analytics.logProperties(any(), any()) } returns true

        val result = controller.logProperties(AnalyticsRequest("12345", mockk()))

        assertSame(HttpStatus.OK, result.statusCode)
    }

    @Test
    internal fun testLogProperties_AnalyticsReturnsFalse_ReturnsBadRequestResult() {
        every { analytics.logProperties(any(), any()) } returns false

        val result = controller.logProperties(AnalyticsRequest("12345", mockk()))

        assertSame(HttpStatus.BAD_REQUEST, result.statusCode)
    }
}
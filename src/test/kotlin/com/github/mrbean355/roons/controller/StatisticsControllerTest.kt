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

package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.TestClock
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.Date

internal class StatisticsControllerTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var analyticsPropertyRepository: AnalyticsPropertyRepository

    @MockK
    private lateinit var metadataRepository: MetadataRepository
    private lateinit var controller: StatisticsController

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        every { metadataRepository.adminToken } returns "12345"
        controller = StatisticsController(appUserRepository, analyticsPropertyRepository, metadataRepository, TestClock(1_000_000))
    }

    @Test
    internal fun testListProperties_WrongToken_ReturnsUnauthorized() {
        val result = controller.listProperties("1111")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testListProperties_CorrectToken_ReturnsProperties() {
        every { analyticsPropertyRepository.findDistinctProperties() } returns listOf("a", "b", "c")

        val result = controller.listProperties("12345")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(listOf("a", "b", "c"), result.body)
    }

    @Test
    internal fun testGetRecentUsers_WrongToken_ReturnsUnauthorized() {
        val result = controller.getRecentUsers("1111", 5)

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testGetRecentUsers_CorrectToken_ReturnsProperties() {
        every { appUserRepository.countByLastSeenAfter(any()) } returns 999

        val result = controller.getRecentUsers("12345", 5)

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(999, result.body ?: 0)
        val slot = slot<Date>()
        verify { appUserRepository.countByLastSeenAfter(capture(slot)) }
        assertEquals(700_000, slot.captured.time)
    }

    @Test
    internal fun testGetStatistic_WrongToken_ReturnsUnauthorized() {
        val result = controller.getStatistic("1111", "")

        assertSame(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    internal fun testGetStatistic_CorrectToken_PropertyNotFound_ReturnsNotFound() {
        every { analyticsPropertyRepository.findByProperty("abc") } returns emptyList()

        val result = controller.getStatistic("12345", "abc")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testGetStatistic_CorrectToken_PropertyFound_ReturnsValueCountMap() {
        every { analyticsPropertyRepository.findByProperty("abc") } returns listOf(
            mockk { every { value } returns "one" },
            mockk { every { value } returns "two,three" },
            mockk { every { value } returns "one,two,three" },
            mockk { every { value } returns "two,four" }
        )

        val result = controller.getStatistic("12345", "abc")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(4, result.body?.size ?: 0)
        assertEquals(2, result.body?.getValue("one"))
        assertEquals(3, result.body?.getValue("two"))
        assertEquals(2, result.body?.getValue("three"))
        assertEquals(1, result.body?.getValue("four"))
    }
}
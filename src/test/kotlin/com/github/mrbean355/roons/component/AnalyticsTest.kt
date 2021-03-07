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

package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnalyticsTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var analyticsPropertyRepository: AnalyticsPropertyRepository
    private lateinit var analytics: Analytics

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        every { analyticsPropertyRepository.saveAll(any<List<AnalyticsProperty>>()) } returns emptyList()
        analytics = Analytics(appUserRepository, analyticsPropertyRepository)
    }

    @Test
    internal fun testLogProperties_FindsUserById() {
        every { appUserRepository.findByGeneratedId(any()) } returns null

        analytics.logProperties("123", mapOf())

        verify { appUserRepository.findByGeneratedId("123") }
    }

    @Test
    internal fun testLogProperties_UserNotFound_ReturnsFalse() {
        every { appUserRepository.findByGeneratedId(any()) } returns null

        val result = analytics.logProperties("123", mockProperties())

        assertFalse(result)
    }

    @Test
    internal fun testLogProperties_EmptyProperties_ReturnsFalse() {
        every { appUserRepository.findByGeneratedId(any()) } returns mockk()

        val result = analytics.logProperties("123", mapOf())

        assertFalse(result)
    }

    @Test
    internal fun testLogProperties_NonEmptyProperties_FindsExistingProperties() {
        val user = mockk<AppUser>()
        every { appUserRepository.findByGeneratedId(any()) } returns user
        every { analyticsPropertyRepository.findByUserAndPropertyIn(any(), any()) } returns emptyList()

        analytics.logProperties("123", mockProperties())

        verify { analyticsPropertyRepository.findByUserAndPropertyIn(user, listOf("a", "b", "c")) }
    }

    @Test
    internal fun testLogProperties_NonEmptyProperties_SavesUpdatedProperties() {
        val user = mockk<AppUser>()
        every { appUserRepository.findByGeneratedId(any()) } returns user
        every { analyticsPropertyRepository.findByUserAndPropertyIn(any(), any()) } returns listOf(AnalyticsProperty(1, user, "a", "qwe"))

        analytics.logProperties("123", mockProperties())

        val slot = slot<List<AnalyticsProperty>>()
        verify { analyticsPropertyRepository.saveAll(capture(slot)) }
        assertEquals(3, slot.captured.size)
        assertEquals(AnalyticsProperty(1, user, "a", "abc"), slot.captured[0])
        assertEquals(AnalyticsProperty(0, user, "b", "bcd"), slot.captured[1])
        assertEquals(AnalyticsProperty(0, user, "c", "cde"), slot.captured[2])
    }

    private fun mockProperties(): Map<String, String> {
        return mapOf("a" to "abc", "b" to "bcd", "c" to "cde")
    }
}
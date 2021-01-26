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

import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.AppUser
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
internal class UserControllerTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var analyticsPropertyRepository: AnalyticsPropertyRepository
    private lateinit var controller: UserController

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        controller = UserController(appUserRepository, analyticsPropertyRepository)
    }

    @Test
    internal fun testFindPeers_UserNotFound_ReturnsNotFoundResponse() {
        every { appUserRepository.findByGeneratedId("12345") } returns null

        val result = controller.findPeers("12345")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testFindPeers_MatchIdNotFound_ReturnsNotFoundResponse() {
        val appUser = mockk<AppUser>()
        every { appUserRepository.findByGeneratedId("12345") } returns appUser
        every { analyticsPropertyRepository.findByUserAndProperty(appUser, "dota.matchId") } returns null

        val result = controller.findPeers("12345")

        assertSame(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    internal fun testFindPeers_BlankMatchId_ReturnsOkResponseWithEmptyList() {
        val appUser = mockk<AppUser>()
        every { appUserRepository.findByGeneratedId("12345") } returns appUser
        every { analyticsPropertyRepository.findByUserAndProperty(appUser, "dota.matchId") } returns AnalyticsProperty(1, appUser, "dota.matchId", "")

        val result = controller.findPeers("12345")

        assertSame(HttpStatus.OK, result.statusCode)
        assertTrue(result.body!!.isEmpty())
    }

    @Test
    internal fun testFindPeers_NonBlankMatchId_ReturnsHeroNamesOfPeers(
        @MockK user1: AppUser,
        @MockK user2: AppUser,
        @MockK user3: AppUser,
    ) {
        val appUser = mockk<AppUser>()
        every { appUserRepository.findByGeneratedId("12345") } returns appUser
        every { analyticsPropertyRepository.findByUserAndProperty(appUser, "dota.matchId") } returns AnalyticsProperty(1, appUser, "dota.matchId", "54321")
        every { analyticsPropertyRepository.findByPropertyAndValue("dota.matchId", "54321") } returns listOf(
            mockk { every { user } returns user1 },
            mockk { every { user } returns user2 },
            mockk { every { user } returns user3 }
        )
        every { analyticsPropertyRepository.findByUserAndProperty(user1, "dota.heroName") } returns mockk { every { value } returns "phoenix" }
        every { analyticsPropertyRepository.findByUserAndProperty(user2, "dota.heroName") } returns mockk { every { value } returns "brewmaster" }
        every { analyticsPropertyRepository.findByUserAndProperty(user3, "dota.heroName") } returns mockk { every { value } returns "oracle" }

        val result = controller.findPeers("12345")

        assertSame(HttpStatus.OK, result.statusCode)
        assertEquals(3, result.body.orEmpty().size)
        assertEquals(listOf("phoenix", "brewmaster", "oracle"), result.body)
    }
}
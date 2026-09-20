package com.github.mrbean355.roons.service

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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnalyticsServiceTest {
    @MockK
    private lateinit var appUserRepository: AppUserRepository

    @MockK
    private lateinit var analyticsPropertyRepository: AnalyticsPropertyRepository
    private lateinit var analyticsService: AnalyticsService

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        every { analyticsPropertyRepository.saveAll(any<List<AnalyticsProperty>>()) } returns emptyList()
        analyticsService = AnalyticsService(appUserRepository, analyticsPropertyRepository)
    }

    @Test
    internal fun testLogProperties_FindsUserById() {
        every { appUserRepository.findByGeneratedId(any()) } returns null

        analyticsService.logProperties("123", mapOf())

        verify { appUserRepository.findByGeneratedId("123") }
    }

    @Test
    internal fun testLogProperties_UserNotFound_ReturnsFalse() {
        every { appUserRepository.findByGeneratedId(any()) } returns null

        val result = analyticsService.logProperties("123", mockProperties())

        assertFalse(result)
    }

    @Test
    internal fun testLogProperties_EmptyProperties_ReturnsFalse() {
        every { appUserRepository.findByGeneratedId(any()) } returns mockk()

        val result = analyticsService.logProperties("123", mapOf())

        assertFalse(result)
    }

    @Test
    internal fun testLogProperties_NonEmptyProperties_FindsExistingProperties() {
        val user = mockk<AppUser>()
        every { appUserRepository.findByGeneratedId(any()) } returns user
        every { analyticsPropertyRepository.findByUserAndPropertyIn(any(), any()) } returns emptyList()

        analyticsService.logProperties("123", mockProperties())

        verify { analyticsPropertyRepository.findByUserAndPropertyIn(user, listOf("a", "b", "c")) }
    }

    @Test
    internal fun testLogProperties_NonEmptyProperties_SavesUpdatedProperties() {
        val user = mockk<AppUser>()
        every { appUserRepository.findByGeneratedId(any()) } returns user
        every { analyticsPropertyRepository.findByUserAndPropertyIn(any(), any()) } returns listOf(AnalyticsProperty(1, user, "a", "qwe"))

        analyticsService.logProperties("123", mockProperties())

        val slot = slot<List<AnalyticsProperty>>()
        verify { analyticsPropertyRepository.saveAll(capture(slot)) }
        assertEquals(3, slot.captured.size)
        assertEquals(AnalyticsProperty(1, user, "a", "abc"), slot.captured[0])
        assertEquals(AnalyticsProperty(0, user, "b", "bcd"), slot.captured[1])
        assertEquals(AnalyticsProperty(0, user, "c", "cde"), slot.captured[2])
    }

    @Test
    internal fun testFindDistinctProperties_DelegatesToRepository() {
        every { analyticsPropertyRepository.findDistinctProperties() } returns listOf("prop1", "prop2")

        assertEquals(listOf("prop1", "prop2"), analyticsService.findDistinctProperties())
    }

    @Test
    internal fun testGetStatistic_NotFound_ReturnsNull() {
        every { analyticsPropertyRepository.findByProperty("unknown") } returns emptyList()

        assertNull(analyticsService.getStatistic("unknown"))
    }

    @Test
    internal fun testGetStatistic_Found_GroupsAndCounts() {
        val user = mockk<AppUser>()
        every { analyticsPropertyRepository.findByProperty("favs") } returns listOf(
            AnalyticsProperty(1, user, "favs", "a,b"),
            AnalyticsProperty(2, user, "favs", "b,c")
        )

        val result = analyticsService.getStatistic("favs")

        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 1), result)
    }

    private fun mockProperties(): Map<String, String> {
        return mapOf("a" to "abc", "b" to "bcd", "c" to "cde")
    }
}

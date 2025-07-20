package com.github.mrbean355.roons.repository

import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.AppUser
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface AnalyticsPropertyRepository : CrudRepository<AnalyticsProperty, Int> {

    fun findByUserAndPropertyIn(user: AppUser, properties: List<String>): List<AnalyticsProperty>

    @Query("SELECT DISTINCT property from AnalyticsProperty ")
    fun findDistinctProperties(): List<String>

    fun findByProperty(property: String): List<AnalyticsProperty>

}
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
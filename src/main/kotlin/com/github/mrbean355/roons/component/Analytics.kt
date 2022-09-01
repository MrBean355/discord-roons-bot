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

package com.github.mrbean355.roons.component

import com.github.mrbean355.roons.AnalyticsProperty
import com.github.mrbean355.roons.repository.AnalyticsPropertyRepository
import com.github.mrbean355.roons.repository.AppUserRepository
import org.springframework.stereotype.Component

@Component
class Analytics(
    private val appUserRepository: AppUserRepository,
    private val analyticsPropertyRepository: AnalyticsPropertyRepository
) {

    fun logProperties(userId: String, properties: Map<String, String>): Boolean {
        val user = appUserRepository.findByGeneratedId(userId)
        if (user == null || properties.isEmpty()) {
            return false
        }

        val existing = analyticsPropertyRepository.findByUserAndPropertyIn(user, properties.keys.toList())
        val entities = properties.map { (property, value) ->
            existing.firstOrNull { it.property == property }?.copy(value = value)
                ?: AnalyticsProperty(0, user, property, value)
        }

        analyticsPropertyRepository.saveAll(entities)
        return true
    }
}
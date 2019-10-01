package com.github.mrbean355.roons.spring

import org.springframework.data.repository.CrudRepository

interface UserEventRepository : CrudRepository<UserEvent, Int> {

    fun findByUserIdAndEventTypeAndEventData(userId: String, eventType: String, eventData: String): UserEvent?
}

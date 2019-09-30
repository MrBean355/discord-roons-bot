package com.github.mrbean355.roons.spring

import org.springframework.data.repository.CrudRepository

interface UserEventRepository : CrudRepository<UserEvent, Int>

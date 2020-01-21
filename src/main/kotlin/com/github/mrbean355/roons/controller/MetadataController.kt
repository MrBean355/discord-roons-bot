package com.github.mrbean355.roons.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metadata")
class MetadataController @Autowired constructor() {

    @RequestMapping("laterVersion", method = [GET])
    fun hasLaterVersion(@RequestParam("version") version: String): ResponseEntity<Boolean> {
        // As of app version 1.8.0, we no longer call this service to check for a new app release.
        // Instead, we use the GitHub API to determine what the latest release is.
        // Therefore, only old apps will call this service. There will always be a new version for them.
        return ResponseEntity.ok(true)
    }
}
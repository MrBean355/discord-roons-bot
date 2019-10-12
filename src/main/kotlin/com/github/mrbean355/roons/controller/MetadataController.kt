package com.github.mrbean355.roons.controller

import com.vdurmont.semver4j.Semver
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Current stable version of the client app. */
private val LATEST_VERSION = Semver("1.6.1")

@RestController
@RequestMapping("/metadata")
class MetadataController {

    @RequestMapping("laterVersion", method = [GET])
    fun hasLaterVersion(@RequestParam("version") version: String): Boolean {
        return Semver(version) < LATEST_VERSION
    }
}
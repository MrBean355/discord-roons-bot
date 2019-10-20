package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.repository.MetadataRepository
import com.vdurmont.semver4j.Semver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metadata")
class MetadataController @Autowired constructor(private val metadataRepository: MetadataRepository) {

    @RequestMapping("laterVersion", method = [GET])
    fun hasLaterVersion(@RequestParam("version") version: String): ResponseEntity<Boolean> {
        val metadata = metadataRepository.findByKey("latest_app_version")
                ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        return ResponseEntity.ok(Semver(version) < Semver(metadata.value))
    }
}
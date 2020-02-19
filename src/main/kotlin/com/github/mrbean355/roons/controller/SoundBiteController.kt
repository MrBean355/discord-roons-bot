package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.discord.SoundStore
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("soundBites/", method = [GET])
class SoundBiteController(private val soundStore: SoundStore) {

    @RequestMapping("list")
    fun list(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(soundStore.listAll())
    }

    @RequestMapping("{name}")
    fun get(@PathVariable("name") name: String): ResponseEntity<Resource> {
        val soundFile = soundStore.getFile(name)
                ?: return ResponseEntity.notFound().build()

        val resource = UrlResource(soundFile.toURI())
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${resource.filename}\"")
                .body(resource)
    }
}

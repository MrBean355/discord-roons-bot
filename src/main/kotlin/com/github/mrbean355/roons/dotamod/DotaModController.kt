package com.github.mrbean355.roons.dotamod

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.getForObject

@RestController
@RequestMapping("/mods")
class DotaModController {

    @RequestMapping
    fun listAll(): List<DotaMod> {
        val restTemplate = RestTemplateBuilder()
                .messageConverters(GsonHttpMessageConverter())
                .build()

        return restTemplate.getForObject<Manifest>("https://dota-mods.s3.us-east-2.amazonaws.com/manifest.json").manifest
    }

    data class Manifest(
            val manifest: List<DotaMod>
    )

    data class DotaMod(
            val name: String,
            val description: String,
            val version: Int,
            val parts: List<DotaModPart>
    )

    data class DotaModPart(
            val name: String,
            val url: String,
            val vpkPath: String
    )
}
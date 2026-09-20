package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DotaModDto
import com.github.mrbean355.roons.security.AdminOnly
import com.github.mrbean355.roons.service.ModService
import com.github.mrbean355.roons.telegram.TelegramNotifier
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mods")
class ModController(
    private val modService: ModService,
    private val telegramNotifier: TelegramNotifier
) {

    @GetMapping
    fun listMods(): List<DotaModDto> = modService.listMods()

    @GetMapping("{key}")
    fun getMod(@PathVariable("key") key: String): ResponseEntity<DotaModDto> {
        val mod = modService.getMod(key)
            ?: return ResponseEntity(NOT_FOUND)

        return ResponseEntity.ok(mod)
    }

    @AdminOnly
    @PatchMapping("{key}")
    fun patchMod(
        @PathVariable("key") key: String,
        @RequestParam("hash") hash: String,
        @RequestParam("size") size: Int,
        @RequestParam("message", required = false) message: String? = null
    ): ResponseEntity<Void> {
        if (!modService.updateMod(key, hash, size)) {
            return ResponseEntity(NOT_FOUND)
        }

        if (message != null) {
            telegramNotifier.sendChannelMessage(message)
        }

        return ResponseEntity.ok().build()
    }

    @AdminOnly
    @GetMapping("refresh")
    fun refreshMods(): ResponseEntity<Void> {
        modService.clearCache()
        return ResponseEntity.ok().build()
    }
}
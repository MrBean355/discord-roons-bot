package com.github.mrbean355.roons.service

import com.github.mrbean355.roons.DotaModDto
import com.github.mrbean355.roons.asDto
import com.github.mrbean355.roons.repository.DotaModRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class ModService(
    private val dotaModRepository: DotaModRepository
) {

    @Transactional(readOnly = true)
    fun listMods(): List<DotaModDto> {
        return dotaModRepository.findAll().map { it.asDto() }
    }

    @Transactional(readOnly = true)
    fun getMod(key: String): DotaModDto? {
        return dotaModRepository.findById(key).getOrNull()?.asDto()
    }

    @Transactional
    fun updateMod(key: String, hash: String, size: Int): Boolean {
        val mod = dotaModRepository.findById(key).getOrNull() ?: return false
        dotaModRepository.save(mod.copy(size = size, hash = hash))
        return true
    }
}

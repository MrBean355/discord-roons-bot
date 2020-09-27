package com.github.mrbean355.roons.controller

import com.github.mrbean355.roons.DotaMod
import com.github.mrbean355.roons.repository.MetadataRepository
import com.github.mrbean355.roons.repository.adminToken
import com.github.mrbean355.roons.telegram.TelegramNotifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetUrlRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

private const val MODS_BUCKET = "dota-mods"
private const val MOD_FILE_NAME = "pak01_dir.vpk"
private const val MOD_INFO_FILE_NAME = "meta.txt"

private const val CACHE_MODS = "mod_list"

@RestController
@RequestMapping("/mods")
class ModController(
        private val s3Client: S3Client,
        private val cacheManager: CacheManager,
        private val metadataRepository: MetadataRepository,
        private val telegramNotifier: TelegramNotifier
) {

    @GetMapping("list")
    @Cacheable(CACHE_MODS)
    fun listMods(): List<DotaMod> {
        val list = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(MODS_BUCKET).build())

        return list.contents()
                .filter { it.key().endsWith(MOD_FILE_NAME) }
                .map {
                    val id = it.key().substringBeforeLast('/')
                    val modInfo = try {
                        s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(MODS_BUCKET).key("$id/$MOD_INFO_FILE_NAME").build())
                                .asByteArray()
                                .decodeToString()
                    } catch (t: Throwable) {
                        ""
                    }

                    val url = s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(MODS_BUCKET).key(it.key()).build())
                    DotaMod(
                            name = modInfo.substringAfter("name=").substringBefore('\n').ifBlank { id },
                            description = modInfo.substringAfter("description=").substringBefore('\n').ifEmpty { "Nondescript." },
                            size = it.size(),
                            hash = it.eTag().removeSurrounding("\""),
                            downloadUrl = url.toExternalForm()
                    )
                }
    }

    @GetMapping("refresh")
    fun refreshMods(@RequestParam("token") token: String): ResponseEntity<Void> {
        if (token != metadataRepository.adminToken) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        cacheManager.getCache(CACHE_MODS)?.let {
            it.clear()
            telegramNotifier.sendMessage("Mods cache has been cleared")
        }
        return ResponseEntity.ok().build()
    }
}
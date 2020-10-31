package com.github.mrbean355.roons.annotation

import org.springframework.cache.annotation.Cacheable

const val DOTA_MOD_CACHE_NAME = "dota_mod_cache"

@Cacheable(DOTA_MOD_CACHE_NAME)
annotation class DotaModCache
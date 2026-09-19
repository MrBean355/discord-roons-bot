rootProject.name = "discord-roons-bot"

pluginManagement {
    plugins {
        kotlin("jvm") version "2.4.0"
        kotlin("plugin.serialization") version "2.4.0"
        id("org.jetbrains.kotlin.plugin.allopen") version "2.4.0"
        id("org.jetbrains.kotlin.plugin.noarg") version "2.4.20"
        id("org.jetbrains.kotlin.plugin.spring") version "2.4.0"
        id("org.jetbrains.kotlin.plugin.jpa") version "2.4.0"
        id("org.springframework.boot") version "4.1.0"
        id("org.sonarqube") version "7.3.1.8318"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

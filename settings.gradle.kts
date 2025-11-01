rootProject.name = "discord-roons-bot"

pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
        kotlin("plugin.serialization") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.allopen") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.noarg") version "2.2.10"
        id("org.jetbrains.kotlin.plugin.spring") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.jpa") version "2.2.21"
        id("org.springframework.boot") version "3.5.5"
        id("org.sonarqube") version "6.3.1.5724"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

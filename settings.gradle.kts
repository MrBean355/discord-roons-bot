rootProject.name = "discord-roons-bot"

pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
        id("org.jetbrains.kotlin.plugin.allopen") version "2.2.0"
        id("org.jetbrains.kotlin.plugin.noarg") version "2.2.0"
        id("org.jetbrains.kotlin.plugin.spring") version "2.2.0"
        id("org.jetbrains.kotlin.plugin.jpa") version "2.2.0"
        id("org.springframework.boot") version "3.5.3"
        id("org.sonarqube") version "6.2.0.5505"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

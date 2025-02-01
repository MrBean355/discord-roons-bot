rootProject.name = "discord-roons-bot"

pluginManagement {
    plugins {
        kotlin("jvm") version "2.1.10"
        id("org.jetbrains.kotlin.plugin.allopen") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.noarg") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.spring") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.jpa") version "2.0.21"
        id("org.springframework.boot") version "3.3.5"
        id("org.sonarqube") version "5.1.0.4882"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

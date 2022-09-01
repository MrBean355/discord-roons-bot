rootProject.name = "discord-roons-bot"

pluginManagement {
    plugins {
        kotlin("jvm") version "1.7.10"
        id("org.jetbrains.kotlin.plugin.allopen") version "1.7.10"
        id("org.jetbrains.kotlin.plugin.noarg") version "1.7.10"
        id("org.jetbrains.kotlin.plugin.spring") version "1.7.10"
        id("org.jetbrains.kotlin.plugin.jpa") version "1.7.10"
        id("org.springframework.boot") version "2.7.3"
        id("org.sonarqube") version "3.4.0.2513"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://m2.dv8tion.net/releases")
        jcenter() // needed for 'lavadsp'
    }
}

include(":api")
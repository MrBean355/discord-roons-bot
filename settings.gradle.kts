rootProject.name = "discord-roons-bot"

pluginManagement {
    plugins {
        kotlin("jvm") version "2.0.10"
        id("org.jetbrains.kotlin.plugin.allopen") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.noarg") version "1.9.23"
        id("org.jetbrains.kotlin.plugin.spring") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.jpa") version "1.9.23"
        id("org.springframework.boot") version "3.2.5"
        id("org.sonarqube") version "5.0.0.4638"
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

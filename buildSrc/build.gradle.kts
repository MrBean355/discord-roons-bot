plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:3.2.3")
    implementation("io.ktor:ktor-client-apache5:3.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
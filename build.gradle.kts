import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", "1.4.21"))
        classpath(kotlin("allopen", "1.4.21"))
        classpath(kotlin("noarg", "1.4.21"))
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.4.1")
    }
}

plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("org.jetbrains.kotlin.plugin.spring") version "1.4.21"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.4.21"
    id("org.springframework.boot") version "2.4.1"
}

group = "com.github.mrbean355"
version = "1.11.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

java {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation("org.springframework.boot:spring-boot-starter-data-rest:2.4.1")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.4.1")
    implementation("mysql:mysql-connector-java:8.0.22")
    implementation("com.google.code.gson:gson:2.8.6")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("net.dv8tion:JDA:4.2.0_225")
    implementation("com.sedmelluq:lavaplayer:1.3.66")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("org.telegram:telegrambots:5.0.1")
    implementation("org.telegram:telegrambots-spring-boot-starter:5.0.1")
}
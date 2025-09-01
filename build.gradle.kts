import com.github.mrbean355.roons.UpdateSoundBitesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.sonarqube.gradle.SonarTask

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.kotlin.plugin.noarg")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.springframework.boot")
    id("org.sonarqube")
    jacoco
    `jvm-test-suite`
}

group = "com.github.mrbean355"
version = "1.22.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<JacocoReport> {
    dependsOn(tasks.test)
    sourceSets(sourceSets.main.get())
    reports {
        xml.required.set(true)
    }
}

tasks.withType<SonarTask> {
    dependsOn(tasks.named("jacocoTestReport"))
}

tasks.register<UpdateSoundBitesTask>("updateSoundBites") {
    destination.set(file("src/main/resources/sounds"))
}

sonar {
    properties {
        property("sonar.projectKey", "discord-roons-bot")
        property("sonar.organization", "admiral-bulldog-sounds")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.2")
            dependencies {
                implementation("io.mockk:mockk:1.14.5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("org.springframework.boot:spring-boot-starter-data-rest:3.5.4")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.5.4")
    implementation("mysql:mysql-connector-java:8.0.33")

    implementation("net.dv8tion:JDA:5.6.1")
    implementation("dev.arbjerg:lavaplayer:2.2.4")
    implementation("com.github.JustRed23:lavadsp:0.7.7-1")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("org.telegram:telegrambots:6.9.7.1")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.9.7.1")

    compileOnly("org.jetbrains:annotations:26.0.2-1")

    runtimeOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
    runtimeOnly("javax.xml.ws:jaxws-api:2.3.1") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
}
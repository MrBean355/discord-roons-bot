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
version = "1.22.3"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}

jacoco {
    toolVersion = "0.8.14"
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
            useJUnitJupiter("6.0.3")
            dependencies {
                implementation("io.mockk:mockk:1.14.9")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation("org.springframework.boot:spring-boot-starter-data-rest:4.0.5")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.0.5")
    implementation("org.springframework.boot:spring-boot-starter-cache:4.0.5")
    implementation("org.postgresql:postgresql:42.7.10")

    implementation("net.dv8tion:JDA:6.4.1")
    implementation("club.minnced:jdave-api:0.1.8")
    implementation("club.minnced:jdave-native-win-x86-64:0.1.8")
    implementation("club.minnced:jdave-native-linux-x86-64:0.1.8")
    implementation("dev.arbjerg:lavaplayer:2.2.6")
    implementation("com.github.JustRed23:lavadsp:0.7.7-1")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("org.telegram:telegrambots:6.9.7.1")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.9.7.1")

    compileOnly("org.jetbrains:annotations:26.1.0")

    runtimeOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.3") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
    runtimeOnly("javax.xml.ws:jaxws-api:2.3.1") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.sonarqube.gradle.SonarTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.kotlin.plugin.noarg")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.springframework.boot")
    id("org.sonarqube")
    jacoco
}

group = "com.github.mrbean355"
version = "1.19.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.9"
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

sonarqube {
    properties {
        property("sonar.projectKey", "discord-roons-bot")
        property("sonar.organization", "admiral-bulldog-sounds")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

dependencies {
    // implementation(project(":api"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("org.springframework.boot:spring-boot-starter-data-rest:3.1.2")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.1.2")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("net.dv8tion:JDA:5.0.0-beta.15")
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    implementation("com.github.natanbc:lavadsp:0.7.7")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("org.telegram:telegrambots:6.8.0")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.8.0")

    compileOnly("org.jetbrains:annotations:24.0.1")

    runtimeOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.0") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
    runtimeOnly("javax.xml.ws:jaxws-api:2.3.1") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
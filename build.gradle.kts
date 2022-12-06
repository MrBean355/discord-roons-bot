/*
 * Copyright 2022 Michael Johnston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.sonarqube.gradle.SonarQubeTask

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
version = "1.18.0-SNAPSHOT"

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
    vendor.set(JvmVendorSpec.ADOPTOPENJDK)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.withType<JacocoReport> {
    dependsOn(tasks.test)
    sourceSets(sourceSets.main.get())
    reports {
        xml.required.set(true)
    }
}

tasks.withType<SonarQubeTask> {
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.springframework.boot:spring-boot-starter-data-rest:3.0.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.7.5")
    implementation("mysql:mysql-connector-java:8.0.31")
    implementation("com.google.code.gson:gson:2.10")

    implementation("net.dv8tion:JDA:5.0.0-alpha.18")
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    implementation("com.github.natanbc:lavadsp:0.7.7")
    implementation("com.vdurmont:semver4j:3.1.0")
    implementation("org.telegram:telegrambots:6.1.0")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.1.0")

    runtimeOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.0") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }
    runtimeOnly("javax.xml.ws:jaxws-api:2.3.1") {
        because("JAXB APIs are considered to be Java EE APIs and are completely removed from JDK 11")
    }

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}
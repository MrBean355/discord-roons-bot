plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "12"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-rest:2.7.3")
}
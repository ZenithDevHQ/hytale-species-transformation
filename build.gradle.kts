plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "dev.zenith"
version = "1.0.0"

repositories {
    mavenCentral()
    // Add Hytale modding repository when available
    // maven("https://repo.hytalemodding.com/releases")
}

dependencies {
    // Hytale Server API - placeholder until official release
    // compileOnly("com.hytale:server-api:1.0.0")

    // For now, we'll use placeholder annotations
    compileOnly("org.jetbrains:annotations:24.1.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("SpeciesTransformation")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

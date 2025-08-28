plugins {
    id("java-library")
    alias(libs.plugins.avro)
}

description = "Core business logic and domain models"

dependencies {
    // API dependencies - exposed to consumers
    api(libs.guava)
    api(libs.slf4j.api)
    api(libs.jakarta.validation)
    api(libs.avro)

    // Implementation dependencies - internal to this module
    implementation(libs.commons.lang3)
    implementation(libs.bundles.jackson)
    implementation(libs.kafka.clients)

    // Test dependencies
    testImplementation(libs.logback.classic)
}

// Configure Avro plugin
avro {
    fieldVisibility.set("PRIVATE")
}

// Configure custom generateAvroJava task
tasks.named("generateAvroJava", com.github.davidmc24.gradle.plugin.avro.GenerateAvroJavaTask::class) {
    source("src/main/java/serialization")
}

// Skip code quality checks for now due to generated Avro files
tasks.checkstyleMain {
    enabled = false
}

tasks.spotbugsMain {
    enabled = false
}
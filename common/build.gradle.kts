plugins {
    id("java-library")
}

description = "Core business logic and domain models"

dependencies {
    api("com.google.guava:guava:33.3.1-jre")
    api("org.slf4j:slf4j-api:2.0.16")
    api("jakarta.validation:jakarta.validation-api:3.1.0")

    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.0")

    testImplementation("ch.qos.logback:logback-classic:1.5.8")
}

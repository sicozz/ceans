plugins {
    id("java")
    id("com.diffplug.spotless") version "7.0.0.BETA2"
    id("checkstyle")
    id("com.github.spotbugs") version "6.0.26"
    id("jacoco")
    id("jacoco-report-aggregation")
    id("org.owasp.dependencycheck") version "10.0.4"
}

allprojects {
    group = "com.sicozz.ceans"
    version = "1.0.0"
}


// Global dependency vulnerability scanning
dependencyCheck {
    format = "ALL"
    failBuildOnCVSS = 7.0f
    suppressionFile = "gradle/dependency-suppression.xml"
    analyzers {
        assemblyEnabled = false
        nuspecEnabled = false
        nugetconfEnabled = false
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile> {
        options.apply {
            compilerArgs.addAll(listOf(
                "-Werror",
                "-Xlint:all",
                "-Xlint:-processing",
                "-parameters",
                "--enable-preview"
            ))
            encoding = "UTF-8"
            release.set(21)
        }
    }

    tasks.withType<Test> {
        jvmArgs("--enable-preview")
        systemProperty("file.encoding", "UTF-8")
        maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).takeIf { it > 0 } ?: 1
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
        testImplementation("org.assertj:assertj-core:3.26.3")
        testImplementation("org.mockito:mockito-core:5.14.2")
        testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // Spotless configuration for code formatting
    spotless {
        java {
            target("src/**/*.java")
            palantirJavaFormat("2.50.0")
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
            importOrder("java", "javax", "org", "com", "")
            licenseHeaderFile(rootProject.file("gradle/license-header.txt"))
        }
    }

    // Checkstyle configuration
    checkstyle {
        toolVersion = "10.20.1"
        configFile = rootProject.file("gradle/checkstyle.xml")
        isIgnoreFailures = false
        maxWarnings = 0
        maxErrors = 0
    }

    configurations.checkstyle {
        resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
            select("com.google.guava:guava:0")
        }
    }

    // SpotBugs configuration
    spotbugs {
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
        excludeFilter.set(rootProject.file("gradle/spotbugs-exclude.xml"))
        ignoreFailures.set(false)
        toolVersion = "4.8.6"
    }

    tasks.spotbugsMain {
        reports.create("html") {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("reports/spotbugs/main.html"))
        }
        reports.create("sarif") {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("reports/spotbugs/main.sarif"))
        }
    }

    tasks.spotbugsTest {
        reports.create("html") {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("reports/spotbugs/test.html"))
        }
    }

    // Custom tasks
    tasks.register("format") {
        dependsOn("spotlessApply")
        group = "formatting"
        description = "Applies code formatting to all source files"
    }

    tasks.register("formatCheck") {
        dependsOn("spotlessCheck")
        group = "verification"
        description = "Checks that all source files are properly formatted"
    }

    tasks.register("lint") {
        dependsOn("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest")
        group = "verification"
        description = "Runs all linting checks"
    }

    // Ensure formatting and linting run with check
    tasks.check {
        dependsOn("formatCheck", "lint")
    }

    // Ensure clean build
    tasks.build {
        dependsOn("clean")
    }
}

tasks.register("aggregateTestReport", JacocoReport::class) {
    dependsOn(subprojects.map { it.tasks.named("test") })
    additionalSourceDirs.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    sourceDirectories.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    classDirectories.setFrom(subprojects.map { it.sourceSets.main.get().output })
    executionData.setFrom(subprojects.map { it.tasks.jacocoTestReport.get().executionData })
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

plugins {
    kotlin("jvm") version "2.4.21"
    java
    signing
    id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "io.github.nayasis"

fun Project.findReleaseVersionProperty(): String? =
    findProperty("mavenReleaseVersion")?.toString()?.takeUnless { it.isBlank() || it == "unspecified" }

fun Project.findReleaseVersionFromBranch(): String? {
    val branchName = providers.environmentVariable("GITHUB_REF_NAME").orNull?.trim().takeUnless { it.isNullOrBlank() } ?: run {
        try {
            providers.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim().takeUnless { it.isBlank() || it == "HEAD" }
        } catch (_: Exception) {
            return null
        }
    }

    return branchName
        ?.removePrefix("refs/heads/")
        ?.takeIf { it.startsWith("release/") }
        ?.removePrefix("release/")
        ?.takeIf { it.isNotBlank() }
}

version = findReleaseVersionProperty()
    ?: findReleaseVersionFromBranch()
    ?: "0.1.0-SNAPSHOT"

println("\t- release version: ${project.version}")

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.mvel:mvel2:2.5.2.Final")
    implementation("com.sigpwned:chardet4j:77.1.0")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.18.11"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.slf4j:slf4j-api:2.0.7")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation("ch.qos.logback:logback-classic:1.6.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("standardOut", "standardError", "failed")
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

mavenPublishing {
    if(listOf("publishToMavenLocal","publishMavenPublicationToMavenLocal").none{gradle.startParameter.taskNames.contains(it)}) {
        signAllPublications()
    }
    publishToMavenCentral()
    pom {
        name        = project.name
        description = "Basic Kotlin utility library providing common functionality for Kotlin applications."
        url         = "https://github.com/nayasis/basica-kt"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url  = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id    = "nayasis"
                name  = "nayasis"
                email = "nayasis@gmail.com"
            }
        }
        scm {
            url                 = "https://github.com/nayasis/basica-kt"
            connection          = "scm:git:github.com/nayasis/basica-kt.git"
            developerConnection = "scm:git:ssh://github.com/nayasis/basica-kt.git"
        }
    }
}

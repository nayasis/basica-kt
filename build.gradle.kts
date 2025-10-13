import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0"
    java
    signing
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "io.github.nayasis"
version = "0.3.7"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation("org.mvel:mvel2:2.5.2.Final")
    implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("org.slf4j:slf4j-api:2.0.7")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation("ch.qos.logback:logback-classic:1.3.15")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.release.set(8)
}

mavenPublishing {
    // Skip signing for local Maven repository deployment
    if (!gradle.startParameter.taskNames.any {
            it.contains("publishToMavenLocal") || it.contains("publishMavenPublicationToMavenLocal")
        }) {
        signAllPublications()
    }
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    pom {
        name.set(rootProject.name)
        description.set("Basic Kotlin utility library providing common functionality for Kotlin applications.")
        url.set("https://github.com/nayasis/basica-kt")
        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("nayasis")
                name.set("nayasis")
                email.set("nayasis@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:github.com/nayasis/basica-kt.git")
            developerConnection.set("scm:git:ssh://github.com/nayasis/basica-kt.git")
            url.set("https://github.com/nayasis/basica-kt/tree/master")
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-XXLanguage:+BreakContinueInInlineLambdas"))
}
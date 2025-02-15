import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	`java`
	`maven-publish`
	kotlin("jvm") version "1.9.24"
	kotlin("plugin.noarg") version "1.9.24"
}

group = "com.github.nayasis"
version = "0.3.2-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations.all {
	resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
}

java {
	withJavadocJar()
	withSourcesJar()
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {

	implementation("org.mvel:mvel2:2.5.2.Final")
	implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
	implementation("ch.qos.logback:logback-classic:1.3.11")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
	testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
	testImplementation("io.kotest:kotest-assertions-core:5.6.2")

}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"])
		}
	}
}
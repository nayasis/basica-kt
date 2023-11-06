import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	`java`
	`maven-publish`
	kotlin("jvm") version "1.9.20"
	kotlin("plugin.noarg") version "1.9.20"
}

noArg {
	annotation("com.github.nayasis.kotlin.basica.annotation.NoArg")
	invokeInitializers = true
}

group = "com.github.nayasis"
version = "0.2.21-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations.all {
	resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
}

java {
	registerFeature("support") {
		usingSourceSet(sourceSets["main"])
	}
	withJavadocJar()
	withSourcesJar()
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {

	implementation("org.mvel:mvel2:2.4.15.Final")
	implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")

	"supportImplementation"("ch.qos.logback:logback-classic:1.3.5")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("io.github.microutils:kotlin-logging:3.0.5")
//	implementation("au.com.console:kassava:2.0.0")

	// test
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")

	testImplementation("io.kotest:kotest-runner-junit5:5.6.1")
	testImplementation("io.kotest:kotest-assertions-core:5.6.1")

	testImplementation("ch.qos.logback:logback-classic:1.3.5")

}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
			"-Xjsr305=strict"
		)
		jvmTarget = "1.8"
	}
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"])
		}
	}
}
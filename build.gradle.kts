import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	`maven`
	kotlin("jvm") version "1.8.10"
	kotlin("plugin.noarg") version "1.8.10"
	id("org.jetbrains.dokka") version "1.7.20"
}

noArg {
	annotation("com.github.nayasis.kotlin.basica.annotation.NoArg")
	invokeInitializers = true
}

group = "com.github.nayasis"
version = "0.1.12-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations.all {
	resolutionStrategy.cacheDynamicVersionsFor(5, "minutes")
}

java {
	registerFeature("support") {
		usingSourceSet(sourceSets["main"])
	}
}

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
	maven { url = uri("https://jitpack.io") }
}
8888
dependencies {

	implementation("org.mvel:mvel2:2.4.12.Final")
	implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.2")

	"supportImplementation"("ch.qos.logback:logback-classic:1.2.9")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("io.github.microutils:kotlin-logging:3.0.0")
	implementation("au.com.console:kassava:2.1.0")

	// test
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")

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
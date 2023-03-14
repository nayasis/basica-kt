import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	`java`
	`maven-publish`
	kotlin("jvm") version "1.8.10"
	kotlin("plugin.noarg") version "1.8.10"
	id("org.jetbrains.dokka") version "1.8.10"
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
	withJavadocJar()
	withSourcesJar()
}

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
	maven { url = uri("https://jitpack.io") }
}

dependencies {

	implementation("org.mvel:mvel2:2.4.12.Final")
	implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.2")
	implementation("org.objenesis:objenesis:3.2")

//	"supportImplementation"("ch.qos.logback:logback-classic:1.2.9")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("io.github.microutils:kotlin-logging:3.0.5")
	implementation("au.com.console:kassava:2.1.0")

	// test
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
//	implementation("ch.qos.logback:logback-core:1.2.3")
//	implementation("ch.qos.logback:logback-classic:1.2.9")
	implementation("ch.qos.logback:logback-classic:1.4.5")
	implementation("org.slf4j:slf4j-api:2.0.3")

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
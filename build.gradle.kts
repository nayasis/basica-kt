import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.4.30"
	kotlin("plugin.serialization") version "1.4.30"
}

group = "com.github.nayasis"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations.all {
	resolutionStrategy.cacheChangingModulesFor(  0, "seconds" )
	resolutionStrategy.cacheDynamicVersionsFor(  5, "minutes" )
}

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
	maven { url = uri("https://raw.github.com/nayasis/maven-repo/mvn-repo") }
	maven { url = uri("https://jitpack.io") }
}

dependencies {

	// temporary
	implementation( "com.github.nayasis:basica:0.3.6-SNAPSHOT" ){ isChanging = true }

	implementation("org.mvel:mvel2:2.4.8.Final")
	implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
//	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.2")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation( "io.github.microutils:kotlin-logging:1.8.3" )
	implementation("au.com.console:kassava:2.1.0-rc.1")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.1")
	testImplementation("ch.qos.logback:logback-classic:1.2.3")

}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		useIR = true
		freeCompilerArgs = listOf(
			"-Xjsr305=strict",
			"-Xuse-experimental=kotlinx.serialization.ExperimentalSerializationApi"
		)
		jvmTarget = "1.8"
	}
}
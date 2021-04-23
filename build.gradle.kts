import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("maven-publish" )
	kotlin("jvm") version "1.4.32"
	kotlin("plugin.allopen") version "1.4.20"
	kotlin("plugin.noarg") version "1.4.20"
	kotlin("plugin.serialization") version "1.4.32"
}

allOpen {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.MappedSuperclass")
	annotation("javax.persistence.Embeddable")
}

noArg {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.MappedSuperclass")
	annotation("javax.persistence.Embeddable")
	annotation("com.github.nayasis.kotlin.basica.annotation.NoArg")
	invokeInitializers = true
}

group = "com.github.nayasis"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations.all {
	resolutionStrategy.cacheChangingModulesFor(  0, "seconds" )
	resolutionStrategy.cacheDynamicVersionsFor(  5, "minutes" )
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

dependencies {

	// temporary
	implementation( "com.github.nayasis:basica:0.3.6" )

	implementation("org.mvel:mvel2:2.4.12.Final")
	implementation("com.googlecode.juniversalchardet:juniversalchardet:1.0.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
//	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.2")

	"supportImplementation"("ch.qos.logback:logback-classic:1.2.3")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation( "io.github.microutils:kotlin-logging:1.8.3" )
	implementation("au.com.console:kassava:2.1.0")

	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")

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

publishing {
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/nayasis/basica-kt")
			credentials {
				username = "nayasis"
				password = "ac40d9d017262cada8501ce7b01d01754ca431e1"
			}
		}
	}
	publications {
		register<MavenPublication>("gpr") {
			from(components["java"])
		}
	}
}
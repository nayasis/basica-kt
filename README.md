# basica-kt
basic kotlin utility


## Dependency

Currently, only snapshot version is available.

### maven

1. add repository in **pom.xml**.

```xml
<repositories>
  <repository>
    <id>nayasis-maven-repo</id>
    <url>https://raw.github.com/nayasis/maven-repo/mvn-repo</url>
  </repository>
</repositories>
```

2. add dependency in **pom.xml**.

```xml
<dependency>
  <groupId>com.github.nayasis</groupId>
  <artifactId>basica-kt</artifactId>
  <version>0.0.1</version>
</dependency>
```

### gradle

1. add repository in **build.gradle.kts**.

```kotlin
repositories {
  maven { url = uri("https://raw.github.com/nayasis/maven-repo/mvn-repo") }
}
```

2. add dependency in **build.gradle.kts**.

```kotlin
dependencies {
  implementation( "com.github.nayasis:basica-kt:0.0.1-SNAPSHOT" ){ isChanging = true }
}
```
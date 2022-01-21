# basica-kt
basic kotlin utility

[![](https://jitpack.io/v/nayasis/basica-kt.svg)](https://jitpack.io/#nayasis/basica-kt)

## Dependency

### maven

1. add repository in **pom.xml**.
```xml
<repositories>
  <repository>
    <id>jitpack</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

2. add dependency in **pom.xml**.
```xml
<dependency>
  <groupId>com.github.nayasis</groupId>
  <artifactId>basica-kt</artifactId>
  <version>0.1.8</version>
</dependency>
```

### gradle

1. add repository in **build.gradle.kts**.
```kotlin
repositories {
  maven { url = uri("https://jitpack.io") }
}
```

2. add dependency in **build.gradle.kts**.
```kotlin
dependencies {
  implementation( "com.github.nayasis:basica-kt:0.1.8" )
}
```

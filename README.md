# kasey

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.seggan/kasey)
[![Docs](https://img.shields.io/badge/docs-gh--pages-blue)](https://seggan.github.io/kasey/)

A modern Stack Exchange client for Kotlin.

## Add as dependency

### Gradle
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.seggan:kasey:VERSION")
}
```

### Maven
```xml
<dependency>
    <groupId>io.github.seggan</groupId>
    <artifactId>kasey</artifactId>
    <version>VERSION</version>
</dependency>
```

## Example
```kotlin
suspend fun main() {
    val client = Client()
    client.login("user@email.com", "password")
    val room = client.joinRoom(1u) // Sandbox
    val message = room.sendMessage("Hello, world!")
    println(message.content)
    message.edit("Hello, world! (edited)")
    client.close()
}
```

## Credits

The work of having to reverse engineer the chat API was saved by looking at the various different 
existing libraries, particularly [chatexchange](https://github.com/SOBotics/chatexchange) (public domain)
and [sechat](https://github.com/gingershaped/sechat) (LGPL-2.1).
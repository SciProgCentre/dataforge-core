# Module dataforge-context

Context and provider definitions

## Features

 - [context](docs/context.md) : Context is a single extension point for all applications. It allows to configure application and handle its lifecycle.
 - [plugins](docs/plugins.md) : Plugin system is a powerful dependency injection and extensibility mechanism. 
 - [context-serialization](docs/context-serialization.md) : Serializer aggregation for context. Each plugin could supply its own serializer module. 


## Usage

## Artifact:

The Maven coordinates of this project are `space.kscience:dataforge-context:0.11.0-dev-2`.

**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:dataforge-context:0.11.0-dev-2")
}
```

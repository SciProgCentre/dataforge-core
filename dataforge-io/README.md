# Module dataforge-io

IO module

## Features

 - [IO format](src/commonMain/kotlin/space/kscience/dataforge/io/IOFormat.kt) : A generic API for reading something from binary representation and writing it to Binary.
 - [Binary](src/commonMain/kotlin/space/kscience/dataforge/io/Binary.kt) : Multi-read random access binary.
 - [Envelope](src/commonMain/kotlin/space/kscience/dataforge/io/Envelope.kt) : API and implementations for combined data and metadata format.
 - [Tagged envelope](src/commonMain/kotlin/space/kscience/dataforge/io/TaggedEnvelope.kt) : Implementation for binary-friendly envelope format with machine readable tag and forward size declaration.
 - [Tagged envelope](src/commonMain/kotlin/space/kscience/dataforge/io/TaglessEnvelope.kt) : Implementation for text-friendly envelope format with text separators for sections.


## Usage

## Artifact:

The Maven coordinates of this project are `space.kscience:dataforge-io:0.10.0`.

**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:dataforge-io:0.10.0")
}
```

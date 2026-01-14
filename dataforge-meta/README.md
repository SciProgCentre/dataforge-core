# Module dataforge-meta

Meta definition and basic operations on meta

## Features

 - [Meta](src/commonMain/kotlin/space/kscience/dataforge/meta/Meta.kt) : **Meta** is the representation of basic DataForge concept: Metadata, but it also could be called meta-value tree.
 - [Value](src/commonMain/kotlin/space/kscience/dataforge/meta/Value.kt) : **Value** a sum type for different meta values.
 - [Name](src/commonMain/kotlin/space/kscience/dataforge/names/Name.kt) : **Name** is an identifier to access tree-like structure.


## Usage

## Artifact:

The Maven coordinates of this project are `space.kscience:dataforge-meta:0.10.3`.

**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:dataforge-meta:0.10.3")
}
```

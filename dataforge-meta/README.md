# Module dataforge-meta

Meta definition and basic operations on meta.

## Features

### [Name](docs/Name.md) 

**Name** is an identifier to access tree-like structure.

### [Value](docs/Value.md)

**Value** a sum type for different meta values.

The following types are implemented in core (custom ones are also available):
* null
* boolean
* number
* string
* list of values

### [Meta](docs/Meta.md)

**Meta** is the representation of basic DataForge concept: Metadata, but it also could be called meta-value tree.
Each Meta node could hava a node Value as well as a map of named child items.

### [Laminate](docs/Laminate.md)

**Laminate** is a multi-layered meta view with resolution rules.

### [NamePattern](docs/NamePattern.md)

### [Serialization](docs/Serialization.md)

### [MetaDescriptor](docs/MetaDescriptor.md)

### [Meta Type Safety](docs/meta%20type%20safety.md)

**Scheme**, **MetaRef**, and **MetaConverter** provide type-safe access to dynamic Meta structures.

## Usage

## Artifact:

The Maven coordinates of this project are `space.kscience:dataforge-meta:0.11.0-dev-6`.

**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:dataforge-meta:0.11.0-dev-6")
}
```

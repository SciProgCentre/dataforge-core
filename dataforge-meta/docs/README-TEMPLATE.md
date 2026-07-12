# Module dataforge-meta

Meta definition and basic operations on meta.

## Features

### [Name](src/commonMain/kotlin/space/kscience/dataforge/names/Name.kt) 

**Name** is an identifier to access tree-like structure.

### [Meta](src/commonMain/kotlin/space/kscience/dataforge/meta/Meta.kt)

**Meta** is the representation of basic DataForge concept: Metadata, but it also could be called meta-value tree.
Each Meta node could hava a node Value as well as a map of named child items.

### [Value](src/commonMain/kotlin/space/kscience/dataforge/meta/Value.kt)

**Value** a sum type for different meta values.

The following types are implemented in core (custom ones are also available):
* null
* boolean
* number
* string
* list of values

### [NamePattern](docs/NamePattern.md)

## Usage

${artifact}

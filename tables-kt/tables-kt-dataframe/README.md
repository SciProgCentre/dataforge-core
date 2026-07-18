# Module tables-kt-dataframe

An integration to convert DataForg tables to DataFrame and back.

## Features

 - [asDataFrame](src/main/kotlin/space/kscience/dataforge/dataframe/TableAsDataFrame.kt) : Conversion from DataForge Table to DataFrame via `asDataFrame()` extension function.
 - [asTable](src/main/kotlin/space/kscience/dataforge/dataframe/DataFrameAsTable.kt) : Conversion from DataFrame to DataForge Table via `asTable()` extension function.


## Usage

## Artifact:

The Maven coordinates of this project are `space.kscience:tables-kt-dataframe:0.11.1`.

**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("space.kscience:tables-kt-dataframe:0.11.1")
}
```

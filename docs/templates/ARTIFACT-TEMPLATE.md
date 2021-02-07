> #### Artifact:
>
> This module artifact: `${group}:${name}:${version}`.
>
> Bintray release version:        [ ![Download](https://api.bintray.com/packages/mipt-npm/dataforge/${name}/images/download.svg) ](https://bintray.com/mipt-npm/kscience/${name}/_latestVersion)
>
> Bintray development version:    [ ![Download](https://api.bintray.com/packages/mipt-npm/dev/${name}/images/download.svg) ](https://bintray.com/mipt-npm/dev/${name}/_latestVersion)
>
> **Gradle:**
>
> ```gradle
> repositories {
>     maven { url 'https://dl.bintray.com/mipt-npm/dataforge' }
>     maven { url 'https://dl.bintray.com/mipt-npm/dev' }
> }
> 
> dependencies {
>     implementation '${group}:${name}:${version}'
> }
> ```
> **Gradle Kotlin DSL:**
>
> ```kotlin
> repositories {
>     maven("https://dl.bintray.com/mipt-npm/kscience")
>     maven("https://dl.bintray.com/mipt-npm/dev")
> }
> 
> dependencies {
>     implementation("${group}:${name}:${version}")
> }
> ```
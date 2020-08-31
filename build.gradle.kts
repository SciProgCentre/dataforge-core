plugins {
    id("kscience.publish") apply false
    id("org.jetbrains.dokka") version "1.4.0-rc"
    id("org.jetbrains.changelog") version "0.4.0"
}

val dataforgeVersion by extra("0.1.9-dev-2")

val bintrayRepo by extra("dataforge")
val githubProject by extra("dataforge-core")
val spaceRepo by extra("https://maven.jetbrains.space/mipt-npm/p/df/maven")

allprojects {
    group = "hep.dataforge"
    version = dataforgeVersion

    repositories {
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "kscience.publish")
    apply(plugin = "org.jetbrains.dokka")
}
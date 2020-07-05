
plugins {
    val toolsVersion = "0.5.0"
    id("scientifik.mpp") version toolsVersion apply false
    id("scientifik.jvm") version toolsVersion apply false
    id("scientifik.publish") version toolsVersion apply false
    id("org.jetbrains.dokka") version "0.10.1"
}

val dataforgeVersion by extra("0.1.8")

val bintrayRepo by extra("dataforge")
val githubProject by extra("dataforge-core")

allprojects {
    group = "hep.dataforge"
    version = dataforgeVersion

    repositories {
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "scientifik.publish")
    apply(plugin = "org.jetbrains.dokka")
}
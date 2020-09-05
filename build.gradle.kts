plugins {
    id("ru.mipt.npm.publish") apply false
    id("org.jetbrains.changelog") version "0.4.0"
}

val dataforgeVersion by extra("0.1.9-dev-5")

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
    apply(plugin = "ru.mipt.npm.publish")
    apply(plugin = "org.jetbrains.dokka")
}
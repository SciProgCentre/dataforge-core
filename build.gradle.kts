plugins {
    id("ru.mipt.npm.project")
}

val dataforgeVersion by extra("0.2.0-dev-3")

val bintrayRepo by extra("dataforge")
val githubProject by extra("dataforge-core")
val spaceRepo by extra("https://maven.jetbrains.space/mipt-npm/p/df/maven")

allprojects {
    group = "hep.dataforge"
    version = dataforgeVersion

    apply(plugin = "org.jetbrains.dokka")

    repositories {
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "ru.mipt.npm.publish")
}
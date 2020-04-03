
plugins {
    val toolsVersion = "0.4.1"
    id("scientifik.mpp") version toolsVersion apply false
    id("scientifik.jvm") version toolsVersion apply false
    id("scientifik.publish") version toolsVersion apply false
}

val dataforgeVersion by extra("0.1.6-dev-2")

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
}
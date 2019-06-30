plugins {
    id("scientifik.mpp") apply false
    id("scientifik.publish") apply false
}

val dataforgeVersion by extra("0.1.3-dev-8")

val bintrayRepo by extra("dataforge")
val vcs by extra("https://github.com/mipt-npm/dataforge-core")

allprojects {
    repositories {
        jcenter()
        maven("https://kotlin.bintray.com/kotlinx")
    }

    group = "hep.dataforge"
    version = dataforgeVersion
}

subprojects {
    if (name.startsWith("dataforge")) {
        apply(plugin = "scientifik.publish")
    } 
}
import scientifik.ScientifikExtension

plugins {
    id("scientifik.mpp") version "0.2.5" apply false
    id("scientifik.jvm") version "0.2.5" apply false
    id("scientifik.publish") version "0.2.5" apply false
}

val dataforgeVersion by extra("0.1.5-dev-3")

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
    afterEvaluate {
        extensions.findByType<ScientifikExtension>()?.apply { withDokka() }
    }
}
plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
}

description = "IO module"

kscience {
    useSerialization(sourceSet = ru.mipt.npm.gradle.DependencySourceSet.TEST) {
        cbor()
    }
}

val ioVersion by rootProject.extra("0.2.0-npm-dev-11")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":dataforge-context"))
                api("org.jetbrains.kotlinx:kotlinx-io:$ioVersion")
            }
        }
    }
}
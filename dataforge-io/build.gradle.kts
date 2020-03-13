import scientifik.DependencySourceSet.TEST
import scientifik.serialization

plugins {
    id("scientifik.mpp")
}

description = "IO module"

serialization(sourceSet = TEST){
    cbor()
}

val ioVersion by rootProject.extra("0.2.0-npm-dev-4")

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
import scientifik.DependencySourceSet.TEST
import scientifik.useSerialization

plugins {
    id("scientifik.mpp")
}

description = "IO module"

useSerialization(sourceSet = TEST){
    cbor()
}

val ioVersion by rootProject.extra("0.2.0-npm-dev-7")

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
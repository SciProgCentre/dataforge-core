import scientifik.useSerialization

plugins {
    id("scientifik.mpp")
}

description = "IO module"

useSerialization()

val ioVersion by rootProject.extra("0.2.0-npm-dev-4")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":dataforge-context"))
                api("org.jetbrains.kotlinx:kotlinx-io:$ioVersion")
                //api("org.jetbrains.kotlinx:kotlinx-io-metadata:$ioVersion")
            }
        }
        jvmMain {
            dependencies {
                //api("org.jetbrains.kotlinx:kotlinx-io-jvm:$ioVersion")
            }
        }
        jsMain {
            dependencies {
                //api("org.jetbrains.kotlinx:kotlinx-io-js:$ioVersion")
            }
        }
    }
}
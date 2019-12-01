plugins {
    id("scientifik.mpp")
}

description = "IO module"

scientifik {
    withSerialization()
    //withIO()
}

val ioVersion by rootProject.extra("0.2.0-npm-dev-3")

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
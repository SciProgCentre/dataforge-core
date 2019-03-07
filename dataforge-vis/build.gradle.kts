plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-io"))
            }
        }
        val jvmMain by getting {
            dependencies {
                //api("no.tornado:tornadofx:1.7.18")
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
    }
}
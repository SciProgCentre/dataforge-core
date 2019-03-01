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
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
    }
}
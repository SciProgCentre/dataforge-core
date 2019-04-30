plugins {
    `multiplatform-config`
}

val coroutinesVersion: String = Versions.coroutinesVersion

kotlin {
    jvm()
    js()
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-meta"))
                api(kotlin("reflect"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
            }
        }

        val jvmMain by getting{
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }

        val jsMain by getting{
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
            }
        }
    }
}
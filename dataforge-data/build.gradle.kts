plugins {
    id("scientifik.mpp")
}

val coroutinesVersion: String = Scientifik.coroutinesVersion

kotlin {
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-meta"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
            }
        }

        val jvmMain by getting{
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api(kotlin("reflect"))
            }
        }

        val jsMain by getting{
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
            }
        }
    }
}
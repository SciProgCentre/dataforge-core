plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-workspace"))
                implementation("org.jetbrains.kotlin:kotlin-scripting-common")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
            }
        }
    }
}
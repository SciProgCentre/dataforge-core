plugins {
    id("scientifik.mpp")
}

val htmlVersion by rootProject.extra("0.6.12")

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-output"))
                api("org.jetbrains.kotlinx:kotlinx-html-common:$htmlVersion")
            }
        }
        val jsMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-html-js:$htmlVersion")
            }
        }
        val jvmMain by getting{
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-html-jvm:$htmlVersion")
            }
        }
    }
}
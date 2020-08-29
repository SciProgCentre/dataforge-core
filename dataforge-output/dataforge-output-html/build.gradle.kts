plugins {
    id("kscience.mpp")
}

val htmlVersion by rootProject.extra("0.7.2")

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-output"))
                api("org.jetbrains.kotlinx:kotlinx-html:$htmlVersion")
            }
        }
    }
}
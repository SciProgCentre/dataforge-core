plugins {
    kotlin("multiplatform")
}

repositories {
    jcenter()
}

kotlin {
    jvm()
    js()
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-meta-io"))
            }
        }
    }
}
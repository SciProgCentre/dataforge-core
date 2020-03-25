plugins {
    id("scientifik.mpp")
}

kotlin {
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-io"))
            }
        }
    }
}
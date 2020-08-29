plugins {
    id("kscience.mpp")
}

kotlin {
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-data"))
                api(project(":dataforge-output"))
            }
        }
    }
}
plugins {
    id("kscience.mpp")
}

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-data"))
                api(project(":dataforge-output"))
            }
        }
    }
}
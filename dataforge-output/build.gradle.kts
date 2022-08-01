plugins {
    id("space.kscience.gradle.mpp")
    id("space.kscience.gradle.native")
}

kotlin {
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-context"))
                //api(project(":dataforge-io"))
            }
        }
    }
}
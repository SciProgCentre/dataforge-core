plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
}

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-data"))
                api(project(":dataforge-io"))
            }
        }
    }
}
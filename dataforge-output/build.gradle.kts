plugins {
    `multiplatform-config`
}

kotlin {
    jvm()
    js()
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-io"))
            }
        }
    }
}
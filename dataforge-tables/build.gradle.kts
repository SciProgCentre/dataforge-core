plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
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

readme{
    maturity = ru.mipt.npm.gradle.Maturity.PROTOTYPE
}
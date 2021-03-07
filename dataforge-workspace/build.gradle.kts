plugins {
    id("ru.mipt.npm.gradle.mpp")
    id("ru.mipt.npm.gradle.native")
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

readme{
    maturity = ru.mipt.npm.gradle.Maturity.EXPERIMENTAL
}
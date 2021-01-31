plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
}

kscience{
    useCoroutines()
}

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-meta"))
            }
        }
        jvmMain{
            dependencies{
                api(kotlin("reflect"))
            }
        }
    }
}

readme{
    maturity = ru.mipt.npm.gradle.Maturity.EXPERIMENTAL
}

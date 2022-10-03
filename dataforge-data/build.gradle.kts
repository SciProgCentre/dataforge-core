plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    native()
    useCoroutines()
}

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-meta"))
                api(kotlin("reflect"))
            }
        }
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}

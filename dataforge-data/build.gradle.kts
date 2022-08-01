plugins {
    id("space.kscience.gradle.mpp")
    id("space.kscience.gradle.native")
}

kscience{
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

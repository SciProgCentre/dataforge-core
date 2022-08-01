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
                api(project(":dataforge-context"))
                api(project(":dataforge-data"))
                api(project(":dataforge-io"))
            }
        }
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
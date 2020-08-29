plugins {
    id("kscience.mpp")
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
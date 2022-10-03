plugins {
    id("space.kscience.gradle.mpp")
}

description = "Context and provider definitions"

kscience {
    native()
    useCoroutines()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-meta"))
            }
        }
        jvmMain {
            dependencies {
                api(kotlin("reflect"))
                api("org.slf4j:slf4j-api:1.7.30")
            }
        }
        jsMain {
            dependencies {

            }
        }
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}
import space.kscience.gradle.KScienceVersions

plugins {
    id("space.kscience.gradle.mpp")
}

description = "IO module"

kscience {
    native()
    useSerialization(sourceSet = space.kscience.gradle.DependencySourceSet.TEST) {
        cbor()
    }
}

//val ioVersion by rootProject.extra("0.2.0-npm-dev-11")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":dataforge-context"))
                api("io.ktor:ktor-io:${KScienceVersions.ktorVersion}")
            }
        }
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}
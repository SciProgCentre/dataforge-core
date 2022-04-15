import ru.mipt.npm.gradle.KScienceVersions

plugins {
    id("ru.mipt.npm.gradle.mpp")
    id("ru.mipt.npm.gradle.native")
}

description = "IO module"

kscience {
    useSerialization(sourceSet = ru.mipt.npm.gradle.DependencySourceSet.TEST) {
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
    maturity = ru.mipt.npm.gradle.Maturity.PROTOTYPE
}
import space.kscience.gradle.KScienceVersions

plugins {
    id("space.kscience.gradle.mpp")
}

description = "IO module"

kscience {
    jvm()
    js()
    native()
    useSerialization()
    useSerialization(sourceSet = space.kscience.gradle.DependencySourceSet.TEST) {
        cbor()
    }
    dependencies {
        api(project(":dataforge-context"))
        api("io.ktor:ktor-io:${KScienceVersions.ktorVersion}")
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
}
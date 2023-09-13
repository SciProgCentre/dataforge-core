plugins {
    id("space.kscience.gradle.mpp")
}

description = "IO module"

val ioVersion = "0.2.0"

kscience {
    jvm()
    js()
    native()
    useSerialization()
    useSerialization(sourceSet = space.kscience.gradle.DependencySourceSet.TEST) {
        cbor()
    }
    dependencies {
        api(projects.dataforgeContext)
        api("org.jetbrains.kotlinx:kotlinx-io-core:$ioVersion")
        api("org.jetbrains.kotlinx:kotlinx-io-bytestring:$ioVersion")
        //api("io.ktor:ktor-io:${KScienceVersions.ktorVersion}")
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
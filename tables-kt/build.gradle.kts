plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

description = "A lightweight multiplatform library for tables"

kscience {
    jvm()
    js()
    native()
    wasmJs()
    useContextParameters()
    dependencies {
        api(projects.dataforgeIo)
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}

plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

description = "A lightweight multiplatform library for tables"

allprojects {
    group = "space.kscience"
    version = "0.4.1"
}

kscience{
    jvm()
    js()
    native()
    wasm()
    useContextParameters()
    dependencies {
        api(projects.dataforgeIo)
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}

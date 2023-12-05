plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    jvm()
    js()
    native()
    wasm()
    useCoroutines()
    dependencies {
        api(spclibs.atomicfu)
        api(projects.dataforgeMeta)
        api(kotlin("reflect"))
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}

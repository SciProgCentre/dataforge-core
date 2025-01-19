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
        api(projects.dataforgeMeta)
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}

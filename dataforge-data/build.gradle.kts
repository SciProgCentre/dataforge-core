plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    jvm()
    js()
    native()
    useCoroutines()
    dependencies {
        api(project(":dataforge-meta"))
        api(kotlin("reflect"))
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}

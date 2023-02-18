plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    jvm()
    js()
    native()
    useCoroutines()
    dependencies {
        api(projects.dataforgeContext)
        api(projects.dataforgeData)
        api(projects.dataforgeIo)
    }
    dependencies(jvmTest){
        implementation(spclibs.logback.classic)
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
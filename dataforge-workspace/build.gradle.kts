plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    jvm()
    js()
    native()
    useCoroutines()
    useSerialization{
        protobuf()
    }
    dependencies {
        api(projects.dataforgeContext)
        api(projects.dataforgeData)
        api(projects.dataforgeIo)
    }
    dependencies(jvmTest){
        implementation(spclibs.logback.classic)
        implementation(projects.dataforgeIo.dataforgeIoYaml)
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
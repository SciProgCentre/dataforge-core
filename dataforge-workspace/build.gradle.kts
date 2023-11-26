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
    commonMain{
        dependencies {
            api(projects.dataforgeContext)
            api(projects.dataforgeData)
            api(projects.dataforgeIo)
        }
    }
    jvmTest{
        dependencies {
            implementation(spclibs.logback.classic)
            implementation(projects.dataforgeIo.dataforgeIoYaml)
        }
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
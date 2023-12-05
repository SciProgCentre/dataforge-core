plugins {
    id("space.kscience.gradle.mpp")
}

kscience {
    jvm()
    js()
    native()
    wasm()
    useCoroutines()
    useSerialization {
        protobuf()
    }
    commonMain {
        api(projects.dataforgeContext)
        api(projects.dataforgeData)
        api(projects.dataforgeIo)

    }
    jvmTest {
        implementation(spclibs.logback.classic)
        implementation(projects.dataforgeIo.dataforgeIoYaml)
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
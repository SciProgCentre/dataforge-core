plugins {
    id("space.kscience.gradle.mpp")
}

description = "A framework for pull-based data processing"

kscience {
    jvm()
    js()
    native()
    wasmJs()
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
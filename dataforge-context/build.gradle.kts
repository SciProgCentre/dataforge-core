plugins {
    id("space.kscience.gradle.mpp")
}

description = "Context and provider definitions"

kscience {
    jvm()
    js()
    native()
    wasmJs()
    useCoroutines()
    useSerialization()
    commonMain {
        api(projects.dataforgeMeta)
    }
    jvmMain{
        api(spclibs.kotlin.reflect)
        api(spclibs.slf4j)
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}
plugins {
    id("space.kscience.gradle.mpp")
}

description = "Context and provider definitions"

kscience {
    jvm()
    js()
    native()
    wasm()
    useCoroutines()
    useSerialization()
    commonMain {
        api(projects.dataforgeMeta)
//        api(spclibs.atomicfu)
    }
    jvmMain{
        api(kotlin("reflect"))
        api("org.slf4j:slf4j-api:1.7.30")
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT
}
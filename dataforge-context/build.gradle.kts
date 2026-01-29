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

    feature("context-serialization","src/docs/context-serialization.md"){
        """
            Serializer aggregation for context. Each plugin could supply its own serializer module. 
            Those modules are aggregated into one serializer module in context.
        """.trimIndent()
    }
}
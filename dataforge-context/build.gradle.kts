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

    feature("context", ref = "docs/context.md"){
        """
            Context is a single extension point for all applications. It allows to configure application and handle its lifecycle.
        """.trimIndent()
    }

    feature("plugins", ref = "docs/plugins.md"){
        """
            Plugin system is a powerful dependency injection and extensibility mechanism. 
            It allows to construct required capabilities for specific application in a single space. 
            Also it provides a capability sharing bus. For example one plugin could provide factories for dynamic 
            instantiation in another plugin. 
        """.trimIndent()
    }

    feature("context-serialization","docs/context-serialization.md"){
        """
            Serializer aggregation for context. Each plugin could supply its own serializer module. 
            Those modules are aggregated into one serializer module in context.
        """.trimIndent()
    }
}
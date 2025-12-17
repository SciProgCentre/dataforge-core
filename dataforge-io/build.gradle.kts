import space.kscience.gradle.DependencyConfiguration

plugins {
    id("space.kscience.gradle.mpp")
}

description = "IO module"

kscience {
    jvm()
    js()
    native()
    wasmJs()
    useSerialization()
    useSerialization(
        sourceSet = space.kscience.gradle.DependencySourceSet.TEST,
        configuration = DependencyConfiguration.IMPLEMENTATION
    ) {
        cbor()
    }
    dependencies {
        api(projects.dataforgeContext)
        api(spclibs.kotlinx.io.core)
        api(spclibs.kotlinx.io.bytestring)
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL

    description = """
        Serialization foundation for Meta objects and Envelope processing.
    """.trimIndent()

    feature(
        "io-format",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/io/IOFormat.kt",
        name = "IO format"
    ) {
        """
            A generic API for reading something from binary representation and writing it to Binary.
            
            Similar to KSerializer, but without schema.
        """.trimIndent()
    }

    feature(
        "binary",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/io/Binary.kt",
        name = "Binary"
    ) {
        "Multi-read random access binary."
    }

    feature(
        "envelope",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/io/Envelope.kt",
        name = "Envelope"
    ) {
        """
            API and implementations for combined data and metadata format.
        """.trimIndent()
    }

    feature(
        "envelope.tagged",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/io/TaggedEnvelope.kt",
        name = "Tagged envelope"
    ) {
        """
            Implementation for binary-friendly envelope format with machine readable tag and forward size declaration.
        """.trimIndent()
    }

    feature(
        "envelope.tagless",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/io/TaglessEnvelope.kt",
        name = "Tagged envelope"
    ) {
        """
            Implementation for text-friendly envelope format with text separators for sections.
        """.trimIndent()
    }
}
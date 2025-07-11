plugins {
    id("space.kscience.gradle.mpp")
}

kscience {
    jvm()
    js()
    native()
    wasm()
    useSerialization {
        json()
    }
}

kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation("io.github.optimumcode:json-schema-validator:0.5.2")
            }
        }
    }
}

description = "Meta definition and basic operations on meta"

readme {
    maturity = space.kscience.gradle.Maturity.DEVELOPMENT

    description = """
        Core Meta and Name manipulation module
    """.trimIndent()

    feature(
        "meta",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/meta/Meta.kt",
        name = "Meta"
    ) {
        """
        **Meta** is the representation of basic DataForge concept: Metadata, but it also could be called meta-value tree.
        
        Each Meta node could hava a node Value as well as a map of named child items.
                    
        """.trimIndent()
    }

    feature(
        "value",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/meta/Value.kt",
        name = "Value"
    ) {
        """
        **Value** a sum type for different meta values.
        
        The following types are implemented in core (custom ones are also available):
            * null
            * boolean
            * number
            * string
            * list of values
        """.trimIndent()
    }

    feature(
        "name",
        ref = "src/commonMain/kotlin/space/kscience/dataforge/names/Name.kt",
        name = "Name"
    ) {
        """
        **Name** is an identifier to access tree-like structure.
        """.trimIndent()
    }
}
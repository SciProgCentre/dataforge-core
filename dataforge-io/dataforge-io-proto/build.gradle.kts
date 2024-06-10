plugins {
    id("space.kscience.gradle.mpp")
    id("com.squareup.wire") version "4.9.9"
}

description = "ProtoBuf meta IO"

kscience {
    jvm()
    dependencies {
        api(projects.dataforgeIo)
        api("com.squareup.wire:wire-runtime:4.9.9")
    }
    useSerialization {
        protobuf()
    }
}

wire {
    kotlin {
        sourcePath {
            srcDir("src/commonMain/proto")
        }
    }
}

readme {
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
    description = """
        ProtoBuf Meta representation
    """.trimIndent()
}

plugins {
    id("space.kscience.gradle.mpp")
    alias (libs.plugins.wire)
}

description = "ProtoBuf meta IO"

kscience {
    jvm()
//    js()
    dependencies {
        api(projects.dataforgeIo)
        api(libs.wire.runtime)
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

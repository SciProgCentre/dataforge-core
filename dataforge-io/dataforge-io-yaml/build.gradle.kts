plugins {
    id("space.kscience.gradle.mpp")
}

description = "YAML meta IO"

kscience {
    jvm()
    js()
    native()
    dependencies {
        api(projects.dataforgeIo)
        implementation(libs.kotaml)
    }
    useSerialization()
}

readme {
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
    description = """
        YAML meta converters and Front Matter envelope format
    """.trimIndent()
}

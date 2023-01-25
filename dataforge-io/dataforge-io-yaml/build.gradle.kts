plugins {
    id("space.kscience.gradle.mpp")
}

description = "YAML meta IO"

kscience {
    native()
    useSerialization{
        yamlKt()
    }
}

repositories{
    maven("https://dl.bintray.com/mamoe/yamlkt")
}

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-io"))
            }
        }
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
    description ="""
        YAML meta converters and Front Matter envelope format
    """.trimIndent()
}

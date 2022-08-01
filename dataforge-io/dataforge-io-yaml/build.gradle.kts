plugins {
    id("space.kscience.gradle.mpp")
//    id("space.kscience.gradle.native")
}

description = "YAML meta IO"

kscience {
    useSerialization{
        yamlKt("0.9.0-dev-1")
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

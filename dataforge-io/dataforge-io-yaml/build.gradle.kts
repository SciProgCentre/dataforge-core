plugins {
    id("ru.mipt.npm.gradle.mpp")
//    id("ru.mipt.npm.gradle.native")
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
    maturity = ru.mipt.npm.gradle.Maturity.PROTOTYPE
    description ="""
        YAML meta converters and Front Matter envelope format
    """.trimIndent()
}

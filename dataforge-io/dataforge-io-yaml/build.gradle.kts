plugins {
    id("ru.mipt.npm.mpp")
//    id("ru.mipt.npm.native")
}

description = "YAML meta IO"

kscience {
    useSerialization{
        yamlKt()
    }
}

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-io"))
                //api("net.mamoe.yamlkt:yamlkt:${ru.mipt.npm.gradle.KScienceVersions.Serialization.yamlKtVersion}")
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

plugins {
    id("space.kscience.gradle.mpp")
}

description = "YAML meta IO"

repositories {
    mavenCentral()
}

kscience {
    jvm()
    js()
    native()
    dependencies {
        api(projects.dataforgeIo)
    }
    useSerialization{
        yamlKt()
    }
}

repositories{
    maven("https://dl.bintray.com/mamoe/yamlkt")
}

readme{
    maturity = space.kscience.gradle.Maturity.PROTOTYPE
    description ="""
        YAML meta converters and Front Matter envelope format
    """.trimIndent()
}

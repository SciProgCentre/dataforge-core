plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
}

description = "YAML meta IO"

repositories{
    jcenter()
}

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
//                api("net.mamoe.yamlkt:yamlkt:${ru.mipt.npm.gradle.KScienceVersions.Serialization.yamlKtVersion}")
            }
        }
    }
}

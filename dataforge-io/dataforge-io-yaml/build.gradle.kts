plugins {
    id("ru.mipt.npm.mpp")
}

description = "YAML meta IO"

repositories{
    jcenter()
}

kscience {
    useSerialization()
}

kotlin {
    sourceSets {
        jvmMain{
            dependencies {
                api(project(":dataforge-io"))
                api("net.mamoe.yamlkt:yamlkt:${ru.mipt.npm.gradle.KScienceVersions.Serialization.yamlKtVersion}")
            }
        }
    }
}

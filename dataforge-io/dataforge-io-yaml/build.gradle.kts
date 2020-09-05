plugins {
    id("ru.mipt.npm.jvm")
}

description = "YAML meta IO"

kscience {
    useSerialization {
        yaml()
    }
}

dependencies {
    api(project(":dataforge-io"))
    api("org.yaml:snakeyaml:1.26")
}

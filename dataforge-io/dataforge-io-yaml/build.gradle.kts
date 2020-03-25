import scientifik.serialization

plugins {
    id("scientifik.jvm")
}

description = "YAML meta IO"

serialization{
    yaml()
}

dependencies {
    api(project(":dataforge-io"))
    api("org.yaml:snakeyaml:1.26")
}

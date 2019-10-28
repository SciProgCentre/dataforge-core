plugins {
    id("scientifik.jvm")
}

description = "YAML meta IO"

dependencies{
    api(project(":dataforge-io"))
    api("com.charleskorn.kaml:kaml:0.14.0")
}

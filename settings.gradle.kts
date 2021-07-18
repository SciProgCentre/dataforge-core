pluginManagement {
    repositories {
        maven("https://repo.kotlin.link")
        mavenCentral()
        gradlePluginPortal()
    }

    val toolsVersion = "0.10.0"

    plugins {
        id("ru.mipt.npm.gradle.project") version toolsVersion
        id("ru.mipt.npm.gradle.mpp") version toolsVersion
        id("ru.mipt.npm.gradle.jvm") version toolsVersion
        id("ru.mipt.npm.gradle.js") version toolsVersion
    }
}

include(
    ":dataforge-meta",
    ":dataforge-io",
    ":dataforge-io:dataforge-io-yaml",
    ":dataforge-context",
    ":dataforge-data",
    ":dataforge-workspace",
    ":dataforge-scripting"
)
pluginManagement {
    repositories {
        maven("https://repo.kotlin.link")
        mavenCentral()
        gradlePluginPortal()
    }

    val toolsVersion = "0.9.10"
    val kotlinVersion = "1.5.10"

    plugins {
        id("ru.mipt.npm.gradle.project") version toolsVersion
        id("ru.mipt.npm.gradle.mpp") version toolsVersion
        id("ru.mipt.npm.gradle.jvm") version toolsVersion
        id("ru.mipt.npm.gradle.js") version toolsVersion
        kotlin("jvm") version kotlinVersion
        kotlin("js") version kotlinVersion
    }
}

include(
    ":dataforge-meta",
    ":dataforge-io",
    ":dataforge-io:dataforge-io-yaml",
    ":dataforge-context",
    ":dataforge-data",
//    ":dataforge-output",
    ":dataforge-tables",
    ":dataforge-workspace",
    ":dataforge-exposed",
    ":dataforge-scripting"
)
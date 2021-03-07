pluginManagement {
    repositories {
        maven("https://repo.kotlin.link")
        jcenter()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }

    val toolsVersion = "0.9.0"
    val kotlinVersion = "1.4.31"

    plugins {
        id("ru.mipt.npm.gradle.project") version toolsVersion
        id("ru.mipt.npm.gradle.mpp") version toolsVersion
        id("ru.mipt.npm.gradle.jvm") version toolsVersion
        id("ru.mipt.npm.gradle.js") version toolsVersion
        id("ru.mipt.npm.gradle.publish") version toolsVersion
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
    ":dataforge-scripting"
)
rootProject.name = "dataforge-core"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {

    val toolsVersion: String by extra

    repositories {
        mavenLocal()
        maven("https://repo.kotlin.link")
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
        id("space.kscience.gradle.project") version toolsVersion
        id("space.kscience.gradle.mpp") version toolsVersion
        id("space.kscience.gradle.jvm") version toolsVersion
    }
}

dependencyResolutionManagement {

    val toolsVersion: String by extra

    repositories {
        mavenLocal()
        maven("https://repo.kotlin.link")
        mavenCentral()
    }

    versionCatalogs {
        create("spclibs") {
            from("space.kscience:version-catalog:$toolsVersion")
        }
    }
}

include(
    ":dataforge-meta",
    ":dataforge-io",
    ":dataforge-io:dataforge-io-yaml",
    ":dataforge-io:dataforge-io-proto",
    ":dataforge-context",
    ":dataforge-data",
    ":dataforge-workspace",
    ":dataforge-scripting",
    ":tables-kt:tables-kt-exposed",
    ":tables-kt:tables-kt-dataframe",
    ":tables-kt:tables-kt-jupyter",
    ":tables-kt:tables-kt-csv"
)
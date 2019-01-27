pluginManagement {
    repositories {
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }
}

//enableFeaturePreview("GRADLE_METADATA")

rootProject.name = "dataforge-core"
include(
    ":dataforge-meta",
    ":dataforge-meta-io",
    ":dataforge-context",
    ":dataforge-data",
    ":dataforge-io"
)

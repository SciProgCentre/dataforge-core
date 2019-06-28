pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/mipt-npm/scientifik")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "kotlinx-atomicfu" -> useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${requested.version}")
                "kotlin-multiplatform" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
                "kotlin2js" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
                "org.jetbrains.kotlin.frontend" -> useModule("org.jetbrains.kotlin:kotlin-frontend-plugin:0.0.45")
                "scientifik.mpp", "scientifik.publish" -> useModule("scientifik:gradle-tools:0.1.0")
            }
        }
    }
}

enableFeaturePreview("GRADLE_METADATA")

//rootProject.name = "dataforge-core"
include(
    ":dataforge-meta",
    ":dataforge-io",
    ":dataforge-context",
    ":dataforge-data",
    ":dataforge-output",
    ":dataforge-output-html",
    ":dataforge-workspace",
    ":dataforge-scripting"
)
plugins {
    id("ru.mipt.npm.mpp")
    id("ru.mipt.npm.native")
}

description = "Context and provider definitions"

kscience {
    useCoroutines()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-meta"))
                api("io.github.microutils:kotlin-logging:1.9.0-dev-npm-2")
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect"))
                api("ch.qos.logback:logback-classic:1.2.3")
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
    }
}
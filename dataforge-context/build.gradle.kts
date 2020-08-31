plugins {
    id("kscience.mpp")
}

description = "Context and provider definitions"

kscience {
    useCoroutines()
}

repositories {
    maven("https://maven.pkg.github.com/altavir/kotlin-logging")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-meta"))
                api("io.github.microutils:kotlin-logging:1.9.0-dev-npm")
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
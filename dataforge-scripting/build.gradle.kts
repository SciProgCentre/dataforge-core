plugins {
    id("scientifik.mpp")
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-workspace"))
                implementation(kotlin("scripting-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("scripting-jvm-host-embeddable"))
                implementation(kotlin("scripting-jvm"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:1.2.3")
            }
        }
    }
}
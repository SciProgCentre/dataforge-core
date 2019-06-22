plugins {
    id("scientifik.mpp")
}

description = "IO for meta"


val ioVersion: String = Versions.ioVersion
val serializationVersion: String  = Versions.serializationVersion

kotlin {
    jvm()
    js()
    sourceSets {
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-meta"))
                //implementation 'org.jetbrains.kotlin:kotlin-reflect'
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
                api("org.jetbrains.kotlinx:kotlinx-io:$ioVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }
        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
                api("org.jetbrains.kotlinx:kotlinx-io-jvm:$ioVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
        val jsMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
                api("org.jetbrains.kotlinx:kotlinx-io-js:$ioVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
            }
        }
//        iosMain {
//        }
//        iosTest {
//        }
    }
}
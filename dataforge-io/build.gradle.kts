plugins {
    id("scientifik.mpp")
}

description = "IO module"

scientifik{
    serialization = true
    io = true
}


kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-context"))
                api("org.jetbrains.kotlinx:kotlinx-io:0.1.14")
            }
        }
        jvmMain{
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-io-jvm:0.1.14")
            }
        }
        jsMain{
            dependencies{
                //api(npm("text-encoding"))
                api("org.jetbrains.kotlinx:kotlinx-io-js:0.1.14")
            }
        }
    }
}
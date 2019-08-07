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
        val commonMain by getting{
            dependencies {
                api(project(":dataforge-context"))
            }
        }
        val jsMain by getting{
            dependencies{
                api(npm("text-encoding"))
            }
        }
    }
}
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
            }
        }
        jsMain{
            dependencies{
                api(npm("text-encoding"))
            }
        }
    }
}
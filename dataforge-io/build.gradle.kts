plugins {
    id("scientifik.mpp")
}

description = "IO module"

scientifik{
    withSerialization()
    withIO()
}


kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-context"))
            }
        }
        jvmMain{
            dependencies {

            }
        }
        jsMain{
            dependencies{
                api(npm("text-encoding"))
            }
        }
    }
}
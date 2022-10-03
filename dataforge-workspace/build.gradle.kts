plugins {
    id("space.kscience.gradle.mpp")
}

kscience{
    native()
    useCoroutines()
}

kotlin {
    sourceSets {
        commonMain{
            dependencies {
                api(project(":dataforge-context"))
                api(project(":dataforge-data"))
                api(project(":dataforge-io"))
            }
        }
        jvmTest{
            dependencies{
                implementation("ch.qos.logback:logback-classic:1.4.1")
            }
        }
    }
}

readme{
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
}
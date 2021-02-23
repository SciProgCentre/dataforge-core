plugins {
    id("ru.mipt.npm.gradle.mpp")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":dataforge-workspace"))
                implementation(kotlin("scripting-common"))
            }
        }
        jvmMain{
            dependencies {
                implementation(kotlin("scripting-jvm-host"))
                implementation(kotlin("scripting-jvm"))
            }
        }
        jvmTest {
            dependencies {
                implementation("ch.qos.logback:logback-classic:1.2.3")
            }
        }
    }
}

readme{
    maturity = ru.mipt.npm.gradle.Maturity.PROTOTYPE
}
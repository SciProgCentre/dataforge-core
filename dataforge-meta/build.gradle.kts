plugins {
    kotlin("multiplatform")
}

description = "Meta definition and basic operations on meta"

kotlin {
    jvm()
    js()

    sourceSets {

        val commonMain by getting {
            dependencies {
                api(kotlin("stdlib"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                api(kotlin("stdlib-js"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
//        mingwMain {
//        }
//        mingwTest {
//        }
    }
}

//tasks.withType<Kotlin2JsCompile>{
//    kotlinOptions{
//        metaInfo = true
//        outputFile = "${project.buildDir.path}/js/${project.name}.js"
//        sourceMap = true
//        moduleKind = "umd"
//        main = "call"
//    }
//}